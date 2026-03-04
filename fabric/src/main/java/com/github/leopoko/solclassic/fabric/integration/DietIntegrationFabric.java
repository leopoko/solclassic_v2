package com.github.leopoko.solclassic.fabric.integration;

import com.github.leopoko.solclassic.utils.FoodDecayTracker;
import com.illusivesoulworks.diet.api.DietEvents;
import com.illusivesoulworks.diet.common.component.DietComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Diet MOD連携（Fabric版）。
 * SoL Classicの食事減衰倍率をDietの栄養グループ値にも比例適用する。
 *
 * Diet MODがインストールされている場合のみロード・登録される。
 */
public class DietIntegrationFabric {

    // 再帰呼び出し防止フラグ（サーバーメインスレッドのみで使用されるため同期不要）
    private static boolean isProcessing = false;

    /**
     * DietイベントリスナーをFabric Event APIに登録する。
     * Diet MODがロードされている場合のみ呼び出すこと。
     */
    public static void register() {
        DietEvents.CONSUME_STACK.register(DietIntegrationFabric::onConsumeStack);
    }

    /**
     * Diet MODの食事消費イベントハンドラ。
     * @return true=Dietのデフォルト処理を続行、false=キャンセル
     */
    private static boolean onConsumeStack(ItemStack stack, Player player) {
        // 再帰呼び出し（スケーリング済みの再適用）は干渉しない
        if (isProcessing) return true;

        FoodDecayTracker.DecayInfo info = FoodDecayTracker.getAndClear(player);

        // SoL Classicで追跡されていない食べ物（ブラックリスト等）はDietのデフォルト処理に任せる
        if (info == null) return true;

        float multiplier = info.multiplier();

        // 減衰なし（倍率100%）の場合はDietのデフォルト処理に任せる
        if (multiplier >= 1.0f) return true;

        // 倍率が0より大きい場合、スケーリング済みの値でDietに再適用
        if (multiplier > 0.0f) {
            ItemStack actualFood = info.actualFood();
            FoodProperties props = actualFood.getItem().getFoodProperties();
            if (props != null) {
                int scaledNutrition = (int) (props.getNutrition() * multiplier);
                DietComponents.DIET_TRACKER.maybeGet(player).ifPresent(tracker -> {
                    isProcessing = true;
                    try {
                        tracker.consume(actualFood, scaledNutrition, props.getSaturationModifier());
                    } finally {
                        isProcessing = false;
                    }
                });
            }
        }

        // Dietのデフォルト処理をキャンセル
        return false;
    }
}
