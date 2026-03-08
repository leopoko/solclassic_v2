package com.github.leopoko.solclassic.neoforge.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class SolClassicConfigNeoForge {
    public static final ModConfigSpec SERVER_CONFIG;
    public static final ServerConfig CONFIG;

    static {
        Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        CONFIG = specPair.getLeft();
        SERVER_CONFIG = specPair.getRight();
    }

    public static class ServerConfig {
        // サーバー側で保持する食事履歴の最大保存件数
        public final ModConfigSpec.IntValue maxFoodHistorySize;
        public final ModConfigSpec.IntValue maxShortFoodHistorySize;
        public final ModConfigSpec.DoubleValue longFoodDecayModifiers;
        public final ModConfigSpec.ConfigValue<List<? extends Double>> shortFoodDecayModifiers;
        public final ModConfigSpec.ConfigValue<List<? extends String>> foodBlacklist;
        public final ModConfigSpec.BooleanValue enableWickerBasket;
        public final ModConfigSpec.BooleanValue guaranteeMinimumNutrition;

        public ServerConfig(ModConfigSpec.Builder builder) {
            // NeoForge 1.21.1 ではSERVERコンフィグは config/ ディレクトリにグローバルに保存されます。
            // このファイルは全ワールドで共有されます。
            // ワールドごとに異なる設定を使いたい場合は、このファイルを
            // saves/<ワールド名>/serverconfig/ にコピーして編集してください。
            builder.comment(
                    "Spice of Life: Classic Edition - Server Configuration",
                    "",
                    "NeoForge stores this config globally in the config/ directory.",
                    "This file is shared across all worlds.",
                    "To use different settings per world, copy this file to",
                    "saves/<world_name>/serverconfig/ and edit it there."
            );
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

            guaranteeMinimumNutrition = builder
                    .comment("Guarantee minimum 1 nutrition even when decay reduces it to 0. When false, fully decayed food gives no nutrition.")
                    .define("guaranteeMinimumNutrition", false);

            builder.pop();
        }
    }
}
