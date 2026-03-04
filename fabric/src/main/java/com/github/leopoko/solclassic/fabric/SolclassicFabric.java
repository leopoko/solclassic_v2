package com.github.leopoko.solclassic.fabric;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.fabric.config.SolClassicConfigLoaderFabric;
import com.github.leopoko.solclassic.fabric.integration.DietIntegrationFabric;
import com.github.leopoko.solclassic.fabric.network.FoodEventHandlerFabric;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class SolclassicFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        FoodHistoryHolder.INSTANCE = new FoodEventHandlerFabric();
        ModNetworking.registerPackets();
        Solclassic.init();
        SolClassicConfigLoaderFabric.register();

        // Diet MOD連携: インストールされている場合のみイベントリスナーを登録
        if (FabricLoader.getInstance().isModLoaded("diet")) {
            DietIntegrationFabric.register();
        }



        //ServerLifecycleEvents.SERVER_STARTED.register(server -> SolClassicConfigLoaderFabric.loadConfig(server));

    }
}
