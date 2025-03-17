package com.github.leopoko.solclassic.forge.config;

import com.github.leopoko.solclassic.Solclassic;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = Solclassic.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class SolClassicConfigForge {
    public static final ForgeConfigSpec SERVER_CONFIG;
    public static final ServerConfig CONFIG;

    static {
        Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        CONFIG = specPair.getLeft();
        SERVER_CONFIG = specPair.getRight();
    }

    public static class ServerConfig {
        // サーバー側で保持する食事履歴の最大保存件数
        public final ForgeConfigSpec.IntValue maxFoodHistorySize;
        public final ForgeConfigSpec.IntValue maxShortFoodHistorySize;
        public final ForgeConfigSpec.DoubleValue longFoodDecayModifiers;
        public final ForgeConfigSpec.ConfigValue<List<? extends Double>> shortFoodDecayModifiers;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> foodBlacklist;
        public final ForgeConfigSpec.BooleanValue enableWickerBasket;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("SolClassicSettings");
            maxFoodHistorySize = builder
                    .comment("Maximum number of food history entries to track")
                    .defineInRange("maxFoodHistorySize", 100, 5, 300);

            maxShortFoodHistorySize = builder
                    .comment("Maximum number of food short history entries to track")
                    .defineInRange("maxShortFoodHistorySize", 5, 1, 100);

            longFoodDecayModifiers = builder
                    .comment("Long decay modifiers for food recovery")
                    .defineInRange("longFoodDecayModifiers", 0.01, 0.0, 1.0);

            // 減衰係数のリスト
            shortFoodDecayModifiers = builder
                    .comment("List of decay modifiers for food recovery, applied sequentially")
                    .defineList("shortFoodDecayModifiers", Arrays.asList(1.0, 0.90, 0.75, 0.50, 0.05), o -> o instanceof Double);

            foodBlacklist = builder
                    .comment("List of food items that should not be tracked")
                    .defineList("foodBlacklist", Arrays.asList("minecraft:dried_kelp"), o -> o instanceof String);

            enableWickerBasket = builder
                    .comment("Enable Wicker Basket")
                    .define("enableWickerBasket", true);

            builder.pop();
        }
    }
}
