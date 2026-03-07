package com.github.leopoko.solclassic.neoforge;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.neoforge.config.SolClassicConfigNeoForge;
import com.github.leopoko.solclassic.neoforge.config.SolClassicConfigInitNeoForge;
import com.github.leopoko.solclassic.neoforge.integration.AppleSkinEventHandler;
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

        Solclassic.init();
    }

    /**
     * サーバー設定がロードまたはクライアントに同期された際に、
     * 共通設定データ（SolclassicConfigData）を更新する。
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
    }
}
