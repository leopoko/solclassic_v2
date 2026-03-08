package com.github.leopoko.solclassic.neoforge;

import com.github.leopoko.solclassic.Solclassic;
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
import net.neoforged.neoforge.common.NeoForge;

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

    /**
     * サーバー設定がロードされた際に、共通設定データ（SolclassicConfigData）を更新する。
     * NeoForge 1.21.1 ではSERVERコンフィグは config/ にグローバルに保存されるため、
     * ワールドごとのデフォルト適用は行わない。
     */
    private void onConfigLoading(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SolClassicConfigNeoForge.SERVER_CONFIG) {
            SolClassicConfigInitNeoForge.init();
        }
    }

    private void onConfigReloading(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SolClassicConfigNeoForge.SERVER_CONFIG) {
            SolClassicConfigInitNeoForge.init();
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
