package com.github.leopoko.solclassic.fabric.foodhistory;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.resources.ResourceLocation;

public class FoodHistoryComponentImplFabric implements EntityComponentInitializer {
    // コンポーネントキーを作成（fabric では net.minecraft.resources.ResourceLocation を使用）
    public static final ComponentKey<FoodHistoryComponentFabric> FOOD_HISTORY =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation("solclassic", "food_history"), FoodHistoryComponentFabric.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // プレイヤーに対して FoodHistoryComponent を自動付与
        registry.registerForPlayers(FOOD_HISTORY, player -> new FoodHistoryComponentFabric(), RespawnCopyStrategy.ALWAYS_COPY);
    }
}