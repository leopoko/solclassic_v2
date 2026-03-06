package com.github.leopoko.solclassic.utils;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FoodCalculator {

    public static float LongMultiplier(ItemStack itemstack, Player player) {
        int count = FoodHistoryHolder.INSTANCE.countFoodEaten(player,itemstack);
        return count * SolclassicConfigData.longFoodDecayModifiers;
    }

    public static float ShortMultiplier(ItemStack itemstack, Player player) {
        List<Float> modifiers = SolclassicConfigData.shortFoodDecayModifiers;
        if (modifiers == null || modifiers.isEmpty()) {
            return 1.0f;
        }
        int count = FoodHistoryHolder.INSTANCE.countFoodEatenRecent(player,itemstack, SolclassicConfigData.maxShortFoodHistorySize);
        count = Math.min(count, modifiers.size() - 1);
        return modifiers.get(count);
    }

    public static float CalculateMultiplier(ItemStack itemstack, Player player){
        float multiplier = ShortMultiplier(itemstack, player) - LongMultiplier(itemstack, player);
        multiplier = Math.max(0.0f, multiplier);
        return multiplier;
    }

    public static int CalculateNutrition(int nutrition, float multiplier){
        int minNutrition = SolclassicConfigData.guaranteeMinimumNutrition ? 1 : 0;
        return Math.max(minNutrition, (int) (nutrition * multiplier));
    }
}
