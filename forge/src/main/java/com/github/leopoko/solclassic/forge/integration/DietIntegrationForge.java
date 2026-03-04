package com.github.leopoko.solclassic.forge.integration;

import com.github.leopoko.solclassic.utils.FoodDecayTracker;
import com.illusivesoulworks.diet.api.DietEvent;
import com.illusivesoulworks.diet.common.capability.DietCapability;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Diet MOD連携（Forge版）。
 * SoL Classicの食事減衰倍率をDietの栄養グループ値にも比例適用する。
 *
 * Diet MODがインストールされている場合のみロード・登録される。
 */
public class DietIntegrationForge {

    // 再帰呼び出し防止フラグ（サーバーメインスレッドのみで使用されるため同期不要）
    private static boolean isProcessing = false;

    /**
     * Forgeイベントバスにハンドラを登録する。
     * Diet MODがロードされている場合のみ呼び出すこと。
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(DietIntegrationForge.class);
    }

    @SubscribeEvent
    public static void onConsumeItemStack(DietEvent.ConsumeItemStack event) {
        // 再帰呼び出し（スケーリング済みの再適用）は干渉しない
        if (isProcessing) return;

        Player player = event.getEntity();
        FoodDecayTracker.DecayInfo info = FoodDecayTracker.getAndClear(player);

        // SoL Classicで追跡されていない食べ物（ブラックリスト等）はDietのデフォルト処理に任せる
        if (info == null) return;

        float multiplier = info.multiplier();

        // 減衰なし（倍率100%）の場合はDietのデフォルト処理に任せる
        if (multiplier >= 1.0f) return;

        // Dietのデフォルト処理をキャンセル
        event.setCanceled(true);

        // 倍率が0より大きい場合、スケーリング済みの値でDietに再適用
        if (multiplier > 0.0f) {
            ItemStack actualFood = info.actualFood();
            FoodProperties props = actualFood.getItem().getFoodProperties();
            if (props != null) {
                int scaledNutrition = (int) (props.getNutrition() * multiplier);
                DietCapability.get(player).ifPresent(tracker -> {
                    isProcessing = true;
                    try {
                        tracker.consume(actualFood, scaledNutrition, props.getSaturationModifier());
                    } finally {
                        isProcessing = false;
                    }
                });
            }
        }
    }
}
