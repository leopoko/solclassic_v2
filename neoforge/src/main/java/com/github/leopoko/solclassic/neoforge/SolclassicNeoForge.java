package com.github.leopoko.solclassic.neoforge;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.config.SolclassicGlobalDefaults;
import com.github.leopoko.solclassic.neoforge.config.SolClassicConfigNeoForge;
import com.github.leopoko.solclassic.neoforge.config.SolClassicConfigInitNeoForge;
import com.github.leopoko.solclassic.neoforge.integration.AppleSkinEventHandler;
import com.github.leopoko.solclassic.neoforge.integration.NutritionalBalanceIntegrationNeoForge;
import com.github.leopoko.solclassic.neoforge.integration.NutritionalBalanceTooltipHandlerNeoForge;
import com.github.leopoko.solclassic.neoforge.network.FoodEventHandlerNeoForge;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.logging.Logger;

@Mod(Solclassic.MOD_ID)
public final class SolclassicNeoForge {
    public SolclassicNeoForge(IEventBus modEventBus) {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, SolClassicConfigNeoForge.SERVER_CONFIG);
        modEventBus.addListener(this::clientInit);
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);
        ModNetworking.registerPackets();

        FoodHistoryHolder.INSTANCE = new FoodEventHandlerNeoForge();

        // Nutritional Balance連携：インストールされている場合のみイベントハンドラを登録
        // サーバー側の栄養素計算に使用するため、コンストラクタ（共通初期化）で登録する
        if (ModList.get().isLoaded("nutritionalbalance")) {
            NutritionalBalanceIntegrationNeoForge.register();
        }

        Solclassic.init();
    }

    private static final Logger LOGGER = Logger.getLogger("SolClassic");

    /**
     * サーバー設定がロードまたはクライアントに同期された際に、
     * 共通設定データ（SolclassicConfigData）を更新する。
     *
     * 新規ワールドの場合（configInitialized == false）、
     * グローバルデフォルト設定を読み込んでModConfigSpecの値に適用する。
     * これにより再起動なしでグローバルデフォルトの変更が新規ワールドに反映される。
     */
    private void onConfigLoading(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SolClassicConfigNeoForge.SERVER_CONFIG) {
            // 新規ワールドの場合、グローバルデフォルト設定を動的に適用
            if (!SolClassicConfigNeoForge.CONFIG.configInitialized.get()) {
                applyGlobalDefaults();
                SolClassicConfigNeoForge.CONFIG.configInitialized.set(true);
            }
            SolClassicConfigInitNeoForge.init();
        }
    }

    private void onConfigReloading(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SolClassicConfigNeoForge.SERVER_CONFIG) {
            SolClassicConfigInitNeoForge.init();
        }
    }

    /**
     * グローバルデフォルト設定（config/solclassic-defaults.toml）を読み込み、
     * ModConfigSpecの値に適用する。新規ワールド作成時にのみ呼ばれる。
     * グローバルデフォルトファイルが存在しない場合はハードコードデフォルトが使われるため、
     * 何も変更されない。
     */
    private void applyGlobalDefaults() {
        try {
            SolclassicGlobalDefaults defaults = SolclassicGlobalDefaults.load(FMLPaths.CONFIGDIR.get());
            SolClassicConfigNeoForge.CONFIG.maxFoodHistorySize.set(defaults.maxFoodHistorySize);
            SolClassicConfigNeoForge.CONFIG.maxShortFoodHistorySize.set(defaults.maxShortFoodHistorySize);
            SolClassicConfigNeoForge.CONFIG.longFoodDecayModifiers.set(defaults.longFoodDecayModifiers);
            SolClassicConfigNeoForge.CONFIG.shortFoodDecayModifiers.set(new ArrayList<>(defaults.shortFoodDecayModifiers));
            SolClassicConfigNeoForge.CONFIG.foodBlacklist.set(new ArrayList<>(defaults.foodBlacklist));
            SolClassicConfigNeoForge.CONFIG.enableWickerBasket.set(defaults.enableWickerBasket);
            SolClassicConfigNeoForge.CONFIG.guaranteeMinimumNutrition.set(defaults.guaranteeMinimumNutrition);
            LOGGER.info("SolClassic: グローバルデフォルト設定を新規ワールドに適用しました");
        } catch (Exception e) {
            LOGGER.warning("SolClassic: グローバルデフォルト設定の適用に失敗しました: " + e.getMessage());
        }
    }

    private void clientInit(final FMLClientSetupEvent event) {
        // AppleSkin連携：インストールされている場合のみイベントハンドラを登録
        if (ModList.get().isLoaded("appleskin")) {
            NeoForge.EVENT_BUS.register(new AppleSkinEventHandler());
        }
        // Nutritional Balance ツールチップ連携（クライアント側）
        if (ModList.get().isLoaded("nutritionalbalance")) {
            NutritionalBalanceTooltipHandlerNeoForge.register();
        }
    }
}
