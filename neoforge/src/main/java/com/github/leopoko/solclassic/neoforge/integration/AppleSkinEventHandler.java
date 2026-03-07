package com.github.leopoko.solclassic.neoforge.integration;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import squeek.appleskin.api.event.FoodValuesEvent;

/**
 * AppleSkin MOD連携ハンドラー。
 * ツールチップやHUDに表示される食事回復量を減衰後の値に修正する。
 *
 * WickerBasketの特殊処理：
 * - WickerBasket自体のFoodPropertiesはダミー（nutrition=0, saturation=0）のため、
 *   AppleSkinにはバスケット内の最も栄養価の高い食べ物のFoodPropertiesを渡す必要がある。
 * - defaultFoodProperties: バスケット内の食べ物の元のFoodProperties（減衰前）
 * - modifiedFoodProperties: 減衰適用後のFoodProperties
 */
public class AppleSkinEventHandler {

    /** ゼロ値のFoodProperties（空のバスケット用） */
    private static final FoodProperties ZERO_FOOD_PROPERTIES =
            new FoodProperties.Builder().nutrition(0).saturationModifier(0f).build();

    @SubscribeEvent
    public void onFoodValuesEvent(FoodValuesEvent event) {
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
                event.defaultFoodProperties = ZERO_FOOD_PROPERTIES;
                event.modifiedFoodProperties = ZERO_FOOD_PROPERTIES;
                return;
            }

            // バスケット内の食べ物のFoodPropertiesをdefaultとして設定
            // （WickerBasket自体のダミーFoodProperties 0/0 を上書き）
            FoodProperties innerFoodProperties = itemStack.get(DataComponents.FOOD);
            if (innerFoodProperties != null) {
                event.defaultFoodProperties = innerFoodProperties;
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
        event.modifiedFoodProperties = modifiedProps;
    }
}
