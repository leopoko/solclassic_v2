package com.github.leopoko.solclassic.forge.config;

import com.github.leopoko.solclassic.config.SolclassicConfigData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SolClassicConfigInitForge {
    // Forge の設定ファイルを読み込む
    public static void init() {
        SolclassicConfigData.maxFoodHistorySize = SolClassicConfigForge.CONFIG.maxFoodHistorySize.get();
        SolclassicConfigData.maxShortFoodHistorySize = SolClassicConfigForge.CONFIG.maxShortFoodHistorySize.get();
        SolclassicConfigData.longFoodDecayModifiers = SolClassicConfigForge.CONFIG.longFoodDecayModifiers.get().floatValue();

        List<? extends Double> convertList = SolClassicConfigForge.CONFIG.shortFoodDecayModifiers.get();
        List<Float> convertedfloatList = convertList.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());
        SolclassicConfigData.shortFoodDecayModifiers = convertedfloatList;

        SolclassicConfigData.foodBlacklist = new ArrayList<>(SolClassicConfigForge.CONFIG.foodBlacklist.get());
        SolclassicConfigData.enableWickerBasket = SolClassicConfigForge.CONFIG.enableWickerBasket.get();
    }
}
