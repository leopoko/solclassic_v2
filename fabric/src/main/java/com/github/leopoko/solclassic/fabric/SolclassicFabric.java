package com.github.leopoko.solclassic.fabric;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.fabric.config.SolClassicConfigLoaderFabric;
import com.github.leopoko.solclassic.fabric.network.FoodEventHandlerFabric;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.ModNetworking;
import net.fabricmc.api.ModInitializer;

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



        //ServerLifecycleEvents.SERVER_STARTED.register(server -> SolClassicConfigLoaderFabric.loadConfig(server));

    }
}
