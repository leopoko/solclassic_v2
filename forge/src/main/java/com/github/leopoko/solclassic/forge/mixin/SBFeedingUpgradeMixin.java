package com.github.leopoko.solclassic.forge.mixin;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sophisticated Backpacks (SophisticatedCore) の FeedingUpgrade に直接介入するMixin。
 *
 * Item.getFoodProperties() をグローバルに変更する代わりに、SBの処理に直接介入することで
 * 影響範囲を最小限に抑える。
 *
 * 機能:
 * 1. isEdible: 食事減衰により回復量が0%になった食べ物をFeedingから除外
 * 2. tryFeedingStack: WickerBasketの場合、SBの通常フロー（コピー+shrink）を回避し、
 *    バスケット内から最適な食べ物を直接食べさせる
 *
 * SophisticatedCoreがインストールされていない場合、ターゲットクラスが存在しないため
 * このMixinは自動的にスキップされる（require = 0）。
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper", remap = false)
public class SBFeedingUpgradeMixin {

    /**
     * isEdible の戻り値をインターセプトし、食事減衰で回復量0%の食べ物を除外する。
     *
     * SBのisEdibleは getFoodProperties(stack, player) != null && nutrition >= 1 をチェックするが、
     * SoL Classicの減衰は考慮されない。ここで追加チェックを行い、
     * 減衰後の栄養値が0以下の場合はfalseを返す。
     */
    @Inject(method = "isEdible", at = @At("RETURN"), cancellable = true, require = 0)
    private void solclassic$skipZeroDecayFood(ItemStack stack, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return; // 既に非食用と判定されている
        if (stack.getItem() instanceof WickerBasketItem) return; // WickerBasketは別途処理
        if (FoodHistoryHolder.INSTANCE == null) return; // 未初期化

        // ブラックリスト食品は減衰の影響を受けないためスキップ
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String blacklisted : SolclassicConfigData.foodBlacklist) {
            if (itemId.equals(blacklisted)) return;
        }

        FoodProperties fp = stack.getItem().getFoodProperties();
        if (fp == null) return;

        float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
        int nutrition = FoodCalculator.CalculateNutrition(fp.getNutrition(), multiplier);
        if (nutrition <= 0) {
            cir.setReturnValue(false);
        }
    }

    /**
     * tryFeedingStack の先頭でWickerBasketを検出し、SBの通常フローを回避して
     * 独自の食事処理を行う。
     *
     * SBの通常フロー:
     *   1. コピーを作成して finishUsingItem() を呼ぶ
     *   2. 元のスタックを shrink(1) → WickerBasketが破壊される
     *
     * この問題を回避するため、WickerBasketの場合は:
     *   1. バスケット内の最も栄養価の高い食べ物を選択
     *   2. player.eat() で直接食べさせる（PlayerMixinForgeが減衰を適用）
     *   3. バスケットのNBTから消費した食べ物を削除
     *   4. SBの通常フローをキャンセル
     */
    @Inject(method = "tryFeedingStack", at = @At("HEAD"), cancellable = true, require = 0)
    private void solclassic$handleWickerBasketFeeding(Level level, int hungerLevel, Player player, Integer slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(stack.getItem() instanceof WickerBasketItem)) return;

        // バスケット内の最も栄養価の高い食べ物を取得（減衰考慮済み）
        ItemStack bestFood = WickerBasketItem.getMostNutritiousFood(stack, player);
        if (bestFood.isEmpty()) {
            cir.setReturnValue(false); // バスケットが空 or すべて0%
            return;
        }

        // プレイヤーが食事できるか確認
        if (!player.canEat(false)) {
            cir.setReturnValue(false);
            return;
        }

        // 食べ物を直接食べさせる
        // player.eat() → Player.eat() → foodData.eat() → PlayerMixinForge（減衰適用）
        // → LivingEntity.eat() → addEatEffect（ポーション効果適用）+ shrink（foodCopyのみ）
        ItemStack foodCopy = bestFood.copy();
        foodCopy.setCount(1);
        player.eat(level, foodCopy);

        // バスケットから消費した食べ物を削除（元のスタックのNBTを直接変更）
        WickerBasketItem.shrinkItemFromInventory(stack, bestFood);

        cir.setReturnValue(true); // 給餌成功 → SBの通常フローをスキップ
    }
}
