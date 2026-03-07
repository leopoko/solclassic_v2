package com.github.leopoko.solclassic.fabric.integration;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.api.event.FoodValuesEvent;

/**
 * AppleSkin MOD連携: Fabric版
 * 食事減衰率をAppleSkinのツールチップやHUDに反映するためのイベントハンドラ。
 *
 * WickerBasketの特殊処理：
 * - WickerBasket自体のFoodPropertiesはダミー（nutrition=0, saturation=0）のため、
 *   AppleSkinにはバスケット内の最も栄養価の高い食べ物のFoodPropertiesを渡す必要がある。
 * - defaultFoodComponent: バスケット内の食べ物の元のFoodProperties（減衰前）
 * - modifiedFoodComponent: 減衰適用後のFoodProperties
 */
public class AppleSkinEventHandlerFabric {

    /** ゼロ値のFoodProperties（空のバスケット用） */
    private static final FoodProperties ZERO_FOOD_PROPERTIES =
            new FoodProperties.Builder().nutrition(0).saturationModifier(0f).build();

    /**
     * AppleSkin の FoodValuesEvent にリスナーを登録する。
     * クライアント初期化時に呼び出すこと。
     */
    public static void register() {
        FoodValuesEvent.EVENT.register(AppleSkinEventHandlerFabric::onFoodValuesEvent);
    }

    private static void onFoodValuesEvent(FoodValuesEvent event) {
        Player player = event.player;
        if (player == null) {
            return;
        }

        ItemStack itemStack = event.itemStack;
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

        // WickerBasketの場合は中身の最も栄養価の高い食べ物を対象にする
        if (itemId.equals("solclassic:wicker_basket")) {
            itemStack = WickerBasketItem.getMostNutritiousFood(itemStack, player);
            if (itemStack.isEmpty()) {
                // バスケットが空の場合はゼロ値を設定
                event.defaultFoodComponent = ZERO_FOOD_PROPERTIES;
                event.modifiedFoodComponent = ZERO_FOOD_PROPERTIES;
                return;
            }

            // バスケット内の食べ物のFoodPropertiesをdefaultとして設定
            // （WickerBasket自体のダミーFoodProperties 0/0 を上書き）
            FoodProperties innerFoodProperties = itemStack.get(DataComponents.FOOD);
            if (innerFoodProperties != null) {
                event.defaultFoodComponent = innerFoodProperties;
            }
        }

        // ブラックリストに含まれている場合は減衰を適用しない
        String foodItemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        for (String blacklistedId : SolclassicConfigData.foodBlacklist) {
            if (foodItemId.equals(blacklistedId)) {
                return;
            }
        }

        // 食べ物のFoodPropertiesを取得
        FoodProperties foodProperties = itemStack.get(DataComponents.FOOD);
        if (foodProperties == null) {
            return;
        }

        // 減衰倍率を計算
        float multiplier = FoodCalculator.CalculateMultiplier(itemStack, player);
        int nutrition = FoodCalculator.CalculateNutrition(foodProperties.nutrition(), multiplier);

        // foodProperties.saturation() は絶対値 (nutrition * modifier * 2.0f) なので
        // 元の saturation modifier を逆算する
        float saturationModifier = (foodProperties.nutrition() > 0)
                ? foodProperties.saturation() / ((float) foodProperties.nutrition() * 2.0f)
                : 0f;

        // 減衰後のFoodPropertiesを作成してAppleSkinに渡す
        FoodProperties modifiedProps = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturationModifier)
                .build();
        event.modifiedFoodComponent = modifiedProps;
    }
}
