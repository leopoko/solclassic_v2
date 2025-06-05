package com.github.leopoko.solclassic.config;

import java.util.Arrays;
import java.util.List;

public class SolclassicConfigData {
    public static int maxFoodHistorySize = 100;
    public static int maxShortFoodHistorySize = 5;
    public static float longFoodDecayModifiers = 0.01F;
    public static List<Float> shortFoodDecayModifiers = Arrays.asList(1.0F, 0.9F, 0.75F, 0.5F, 0.05F);
    // Items that will not be affected by food history tracking
    public static List<String> foodBlacklist = Arrays.asList("minecraft:dried_kelp");
    public static boolean enableWickerBasket = true;
}
