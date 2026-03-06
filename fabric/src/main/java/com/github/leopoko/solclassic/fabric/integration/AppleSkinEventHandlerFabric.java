package com.github.leopoko.solclassic.fabric.integration;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.food.FoodValues;

/**
 * AppleSkin MOD連携: Fabric版
 * 食事減衰率をAppleSkinのツールチップに反映するためのイベントハンドラ。
 * Forge版の AppleSkinEventHandler と同等のロジックを実装。
 */
public class AppleSkinEventHandlerFabric {

    /**
     * AppleSkin の FoodValuesEvent にリスナーを登録する。
     * クライアント初期化時に呼び出すこと。
     */
    public static void register() {
        FoodValuesEvent.EVENT.register(AppleSkinEventHandlerFabric::onFoodValuesEvent);
    }

    private static void onFoodValuesEvent(FoodValuesEvent event) {
        Player player = event.player;
        ItemStack itemStack = event.itemStack;

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

        // WickerBasketの場合は中身の最も栄養価の高い食べ物を対象にする
        if (itemId.equals("solclassic:wicker_basket")) {
            WickerBasketItem wickerBasketItem = (WickerBasketItem) itemStack.getItem();
            itemStack = wickerBasketItem.getMostNutritiousFood(itemStack, player);
            if (itemStack.isEmpty()) {
                FoodValues zeroFoodValues = new FoodValues(0, 0);
                event.modifiedFoodValues = zeroFoodValues;
                event.defaultFoodValues = zeroFoodValues;
                return;
            }
        }

        // Fabric版ではバニラAPIを使用（Forgeの getFoodProperties(Player) は利用不可）
        FoodProperties foodProperties = itemStack.getItem().getFoodProperties();
        if (foodProperties == null) return;

        // 減衰倍率を計算し、栄養値に適用
        float multiplier = FoodCalculator.CalculateMultiplier(itemStack, player);
        int nutrition = FoodCalculator.CalculateNutrition(foodProperties.getNutrition(), multiplier);

        FoodValues customFoodValues = new FoodValues(nutrition, foodProperties.getSaturationModifier());

        event.modifiedFoodValues = customFoodValues;
    }
}
