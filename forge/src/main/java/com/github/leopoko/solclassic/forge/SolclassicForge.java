package com.github.leopoko.solclassic.forge;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.config.SolclassicGlobalDefaults;
import com.github.leopoko.solclassic.forge.config.SolClassicConfigForge;
import com.github.leopoko.solclassic.forge.config.SolClassicConfigInitForge;
import com.github.leopoko.solclassic.forge.integration.AppleSkinEventHandler;
import com.github.leopoko.solclassic.forge.integration.DietIntegrationForge;
import com.github.leopoko.solclassic.forge.integration.DietTooltipHandlerForge;
import com.github.leopoko.solclassic.forge.integration.NutritionalBalanceIntegrationForge;
import com.github.leopoko.solclassic.forge.integration.NutritionalBalanceTooltipHandlerForge;
import com.github.leopoko.solclassic.forge.network.FoodEventHandlerForge;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.ModNetworking;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.ArrayList;
import java.util.logging.Logger;

@Mod(Solclassic.MOD_ID)
public final class SolclassicForge {
    @SuppressWarnings("removal") // Forge 1.20.1ではこれらのAPIが非推奨だが、代替APIが未整備のため使用を継続
    public SolclassicForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Solclassic.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SolClassicConfigForge.SERVER_CONFIG);
        modEventBus.addListener(this::clientInit);
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);
        ModNetworking.registerPackets();

        FoodHistoryHolder.INSTANCE = new FoodEventHandlerForge();

        // Diet MOD連携: インストールされている場合のみイベントハンドラを登録
        if (ModList.get().isLoaded("diet")) {
            DietIntegrationForge.register();
        }

        // Nutritional Balance MOD連携: インストールされている場合のみイベントハンドラを登録
        if (ModList.get().isLoaded("nutritionalbalance")) {
            NutritionalBalanceIntegrationForge.register();
        }

        // Run our common setup.
        Solclassic.init();
    }

    private static final Logger LOGGER = Logger.getLogger("SolClassic");

    /**
     * サーバー設定がロードまたはクライアントに同期された際に、
     * 共通設定データ（SolclassicConfigData）を更新する。
     * ForgeConfigSpecのSERVER設定はクライアントへ自動同期されるが、
     * SolclassicConfigDataの静的フィールドは自動更新されないため、
     * ここで明示的に反映する。
     *
     * 新規ワールドの場合（configInitialized == false）、
     * グローバルデフォルト設定を読み込んでForgeConfigSpecの値に適用する。
     * これにより再起動なしでグローバルデフォルトの変更が新規ワールドに反映される。
     */
    private void onConfigLoading(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SolClassicConfigForge.SERVER_CONFIG) {
            // 新規ワールドの場合、グローバルデフォルト設定を動的に適用
            if (!SolClassicConfigForge.CONFIG.configInitialized.get()) {
                applyGlobalDefaults();
                SolClassicConfigForge.CONFIG.configInitialized.set(true);
            }
            SolClassicConfigInitForge.init();
        }
    }

    private void onConfigReloading(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SolClassicConfigForge.SERVER_CONFIG) {
            SolClassicConfigInitForge.init();
        }
    }

    /**
     * グローバルデフォルト設定（config/solclassic-defaults.toml）を読み込み、
     * ForgeConfigSpecの値に適用する。新規ワールド作成時にのみ呼ばれる。
     * グローバルデフォルトファイルが存在しない場合はハードコードデフォルトが使われるため、
     * 何も変更されない。
     */
    private void applyGlobalDefaults() {
        try {
            SolclassicGlobalDefaults defaults = SolclassicGlobalDefaults.load(FMLPaths.CONFIGDIR.get());
            SolClassicConfigForge.CONFIG.maxFoodHistorySize.set(defaults.maxFoodHistorySize);
            SolClassicConfigForge.CONFIG.maxShortFoodHistorySize.set(defaults.maxShortFoodHistorySize);
            SolClassicConfigForge.CONFIG.longFoodDecayModifiers.set(defaults.longFoodDecayModifiers);
            SolClassicConfigForge.CONFIG.shortFoodDecayModifiers.set(new ArrayList<>(defaults.shortFoodDecayModifiers));
            SolClassicConfigForge.CONFIG.foodBlacklist.set(new ArrayList<>(defaults.foodBlacklist));
            SolClassicConfigForge.CONFIG.enableWickerBasket.set(defaults.enableWickerBasket);
            SolClassicConfigForge.CONFIG.guaranteeMinimumNutrition.set(defaults.guaranteeMinimumNutrition);
            SolClassicConfigForge.CONFIG.enableTooltip.set(defaults.enableTooltip);
            SolClassicConfigForge.CONFIG.enableItemDescription.set(defaults.enableItemDescription);
            LOGGER.info("SolClassic: グローバルデフォルト設定を新規ワールドに適用しました");
        } catch (Exception e) {
            LOGGER.warning("SolClassic: グローバルデフォルト設定の適用に失敗しました: " + e.getMessage());
        }
    }

    private void clientInit(final FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("appleskin")) {
            MinecraftForge.EVENT_BUS.register(new AppleSkinEventHandler());
        }
        // Diet MODツールチップ連携: クライアント側でのみ登録
        if (ModList.get().isLoaded("diet")) {
            DietTooltipHandlerForge.register();
        }
        // Nutritional Balance MODツールチップ連携: クライアント側でのみ登録
        if (ModList.get().isLoaded("nutritionalbalance")) {
            NutritionalBalanceTooltipHandlerForge.register();
        }
    }
}
