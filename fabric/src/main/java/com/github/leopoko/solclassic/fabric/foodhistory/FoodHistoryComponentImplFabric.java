package com.github.leopoko.solclassic.fabric.foodhistory;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.resources.ResourceLocation;

public class FoodHistoryComponentImplFabric implements EntityComponentInitializer {
    public static final ComponentKey<FoodHistoryComponentFabric> FOOD_HISTORY =
            ComponentRegistryV3.INSTANCE.getOrCreate(ResourceLocation.fromNamespaceAndPath("solclassic", "food_history"), FoodHistoryComponentFabric.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // プレイヤーに対して FoodHistoryComponent を自動付与
        registry.registerForPlayers(FOOD_HISTORY, player -> new FoodHistoryComponentFabric(), RespawnCopyStrategy.ALWAYS_COPY);
    }
}
