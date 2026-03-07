package com.github.leopoko.solclassic.fabric.client;

import com.github.leopoko.solclassic.fabric.integration.AppleSkinEventHandlerFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class SolclassicFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // AppleSkin連携：インストールされている場合のみイベントハンドラを登録
        if (FabricLoader.getInstance().isModLoaded("appleskin")) {
            AppleSkinEventHandlerFabric.register();
        }
    }
}
