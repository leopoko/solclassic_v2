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
        FoodHistoryHolder.INSTANCE = new FoodEventHandlerFabric();
        ModNetworking.registerPackets();
        Solclassic.init();
        SolClassicConfigLoaderFabric.register();
    }
}
