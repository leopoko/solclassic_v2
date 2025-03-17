package com.github.leopoko.solclassic.forge.integration;

import com.github.leopoko.solclassic.forge.config.SolClassicConfigForge;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.food.FoodValues;

import java.util.ArrayList;
import java.util.List;

public class AppleSkinEventHandler {
    @SubscribeEvent()
    public void onFoodValuesEvent(FoodValuesEvent event) {
        Player player = event.player;
        ItemStack itemStack = event.itemStack;

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

        if (itemId.equals("solclassic:wicker_basket")){
            WickerBasketItem wickerBasketItem = (WickerBasketItem) itemStack.getItem();
            itemStack = wickerBasketItem.getMostNutritiousFood(itemStack, player);
            if (itemStack.isEmpty()) {
                FoodValues zeroFoodValues = new FoodValues(0, 0);
                event.modifiedFoodValues = zeroFoodValues;
                event.defaultFoodValues = zeroFoodValues;
                return;
            }
        }

        FoodProperties foodProperties = itemStack.getFoodProperties(player);

        float multiplier = FoodCalculator.CalculateMultiplier(itemStack, player);
        int nutrition = FoodCalculator.CalculateNutrition(foodProperties.getNutrition(), multiplier);

        FoodValues customFoodValues = new FoodValues(nutrition, foodProperties.getSaturationModifier());

        event.modifiedFoodValues = customFoodValues;
        }
}
