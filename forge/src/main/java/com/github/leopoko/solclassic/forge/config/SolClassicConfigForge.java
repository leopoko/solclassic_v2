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
        // ForgeConfigSpecのデフォルト値はハードコード値を使用。
        // グローバルデフォルトは新規ワールド作成時にonConfigLoadingで動的に適用される。
        // （staticブロックはJVMで1回しか実行されないため、ここでグローバルデフォルトを読むと
        // 再起動なしでは変更が反映されない）
        Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
                .configure(ServerConfig::new);
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
        public final ForgeConfigSpec.BooleanValue guaranteeMinimumNutrition;
        // 新規ワールド検出用の内部フラグ
        public final ForgeConfigSpec.BooleanValue configInitialized;

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
                    .defineList("shortFoodDecayModifiers", Arrays.asList(1.0, 0.9, 0.75, 0.5, 0.05), o -> o instanceof Double);

            foodBlacklist = builder
                    .comment("List of food items that should not be tracked")
                    .defineList("foodBlacklist", Arrays.asList("minecraft:dried_kelp"), o -> o instanceof String);

            enableWickerBasket = builder
                    .comment("Enable Wicker Basket")
                    .define("enableWickerBasket", true);

            guaranteeMinimumNutrition = builder
                    .comment("Guarantee minimum 1 nutrition even when decay reduces it to 0. When false, fully decayed food gives no nutrition.")
                    .define("guaranteeMinimumNutrition", false);

            configInitialized = builder
                    .comment("Internal flag: DO NOT MODIFY. Used to detect new world configs for applying global defaults.")
                    .define("configInitialized", false);

            builder.pop();
        }
    }
}
