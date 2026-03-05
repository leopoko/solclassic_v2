package com.github.leopoko.solclassic.fabric.client;

import com.github.leopoko.solclassic.fabric.integration.DietTooltipHandlerFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class SolclassicFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Diet MODツールチップ連携: クライアント側でのみ登録
        if (FabricLoader.getInstance().isModLoaded("diet")) {
            DietTooltipHandlerFabric.register();
        }
    }
}
