package com.github.leopoko.solclassic.fabric;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.fabric.config.SolClassicConfigLoaderFabric;
import com.github.leopoko.solclassic.fabric.foodhistory.FoodHistoryComponentFabric;
import com.github.leopoko.solclassic.fabric.foodhistory.FoodHistoryComponentImplFabric;
import com.github.leopoko.solclassic.fabric.network.FoodEventHandlerFabric;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.ModNetworking;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.entity.player.Player;

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
