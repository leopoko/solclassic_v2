package com.github.leopoko.solclassic.neoforge.config;

import com.github.leopoko.solclassic.config.SolclassicConfigData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SolClassicConfigInitNeoForge {
    // NeoForge の設定ファイルを読み込む
    public static void init() {
        SolclassicConfigData.maxFoodHistorySize = SolClassicConfigNeoForge.CONFIG.maxFoodHistorySize.get();
        SolclassicConfigData.maxShortFoodHistorySize = SolClassicConfigNeoForge.CONFIG.maxShortFoodHistorySize.get();
        SolclassicConfigData.longFoodDecayModifiers = SolClassicConfigNeoForge.CONFIG.longFoodDecayModifiers.get().floatValue();

        List<? extends Double> convertList = SolClassicConfigNeoForge.CONFIG.shortFoodDecayModifiers.get();
        List<Float> convertedfloatList = convertList.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList());
        SolclassicConfigData.shortFoodDecayModifiers = convertedfloatList;

        SolclassicConfigData.foodBlacklist = new ArrayList<>(SolClassicConfigNeoForge.CONFIG.foodBlacklist.get());
        SolclassicConfigData.enableWickerBasket = SolClassicConfigNeoForge.CONFIG.enableWickerBasket.get();
        SolclassicConfigData.guaranteeMinimumNutrition = SolClassicConfigNeoForge.CONFIG.guaranteeMinimumNutrition.get();
        SolclassicConfigData.enableTooltip = SolClassicConfigNeoForge.CONFIG.enableTooltip.get();
        SolclassicConfigData.enableItemDescription = SolClassicConfigNeoForge.CONFIG.enableItemDescription.get();
    }
}
