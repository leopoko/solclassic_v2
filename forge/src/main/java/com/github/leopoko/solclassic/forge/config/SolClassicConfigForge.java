package com.github.leopoko.solclassic.forge.config;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.config.SolclassicGlobalDefaults;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Solclassic.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class SolClassicConfigForge {
    public static final ForgeConfigSpec SERVER_CONFIG;
    public static final ServerConfig CONFIG;
    private static final SolclassicGlobalDefaults GLOBAL_DEFAULTS;

    static {
        // グローバルデフォルト設定を読み込む（FMLPathsはmod class loading前に初期化済み）
        SolclassicGlobalDefaults loadedDefaults;
        try {
            loadedDefaults = SolclassicGlobalDefaults.load(FMLPaths.CONFIGDIR.get());
        } catch (Exception e) {
            loadedDefaults = new SolclassicGlobalDefaults();
        }
        GLOBAL_DEFAULTS = loadedDefaults;

        Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
                .configure(builder -> new ServerConfig(builder, GLOBAL_DEFAULTS));
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

        public ServerConfig(ForgeConfigSpec.Builder builder, SolclassicGlobalDefaults defaults) {
            builder.push("SolClassicSettings");
            maxFoodHistorySize = builder
                    .comment("Maximum number of food history entries to track")
                    .defineInRange("maxFoodHistorySize", defaults.maxFoodHistorySize, 5, 300);

            maxShortFoodHistorySize = builder
                    .comment("Maximum number of food short history entries to track")
                    .defineInRange("maxShortFoodHistorySize", defaults.maxShortFoodHistorySize, 1, 100);

            longFoodDecayModifiers = builder
                    .comment("Long decay modifiers for food recovery")
                    .defineInRange("longFoodDecayModifiers", defaults.longFoodDecayModifiers, 0.0, 1.0);

            // 減衰係数のリスト
            shortFoodDecayModifiers = builder
                    .comment("List of decay modifiers for food recovery, applied sequentially")
                    .defineList("shortFoodDecayModifiers", new ArrayList<>(defaults.shortFoodDecayModifiers), o -> o instanceof Double);

            foodBlacklist = builder
                    .comment("List of food items that should not be tracked")
                    .defineList("foodBlacklist", new ArrayList<>(defaults.foodBlacklist), o -> o instanceof String);

            enableWickerBasket = builder
                    .comment("Enable Wicker Basket")
                    .define("enableWickerBasket", defaults.enableWickerBasket);

            guaranteeMinimumNutrition = builder
                    .comment("Guarantee minimum 1 nutrition even when decay reduces it to 0. When false, fully decayed food gives no nutrition.")
                    .define("guaranteeMinimumNutrition", defaults.guaranteeMinimumNutrition);

            builder.pop();
        }
    }
}
