package com.github.leopoko.solclassic.forge.mixin;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 食べ物アイテム全般に対するSophisticated Backpacks互換性対応。
 *
 * SBのFeeding UpgradeはForge固有の getFoodProperties(ItemStack, LivingEntity) で食べ物を判定する。
 * 食事減衰により回復量が0%になった食べ物は食べる意味がないため、
 * プレイヤーインベントリ外（SBバックパック等）にある場合はnullを返して除外する。
 *
 * プレイヤーインベントリ内の食べ物にはこの処理を適用しない（ツールチップやAppleSkin表示のため）。
 */
@Mixin(Item.class)
public class FoodItemMixinForge {

    @Inject(
            method = "getFoodProperties(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/food/FoodProperties;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void solclassic$skipZeroDecayFood(ItemStack stack, @Nullable LivingEntity entity, CallbackInfoReturnable<FoodProperties> cir) {
        // WickerBasketは専用のMixin（WickerBasketMixinForge）で処理する
        if ((Item)(Object)this instanceof WickerBasketItem) return;

        // 食べ物でないアイテムはスキップ
        FoodProperties baseFp = ((Item)(Object)this).getFoodProperties();
        if (baseFp == null) return;

        // FoodHistoryHolderが未初期化の場合はスキップ（MOD初期化中など）
        if (FoodHistoryHolder.INSTANCE == null) return;

        if (entity instanceof Player player) {
            // ブラックリスト食品は減衰の影響を受けないためスキップ
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            for (String blacklisted : SolclassicConfigData.foodBlacklist) {
                if (itemId.equals(blacklisted)) return;
            }

            // プレイヤーインベントリ（メイン+オフハンド）内にある場合はスキップ
            // ツールチップやAppleSkin等の表示にはバニラのFoodPropertiesが必要
            for (ItemStack invStack : player.getInventory().items) {
                if (invStack == stack) return;
            }
            for (ItemStack invStack : player.getInventory().offhand) {
                if (invStack == stack) return;
            }

            // プレイヤーインベントリ外（SBバックパック等）の場合
            // 減衰倍率を計算し、回復量が0になる食べ物はnullを返してSBのFeedingから除外
            float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
            int nutrition = FoodCalculator.CalculateNutrition(baseFp.getNutrition(), multiplier);
            if (nutrition <= 0) {
                cir.setReturnValue(null);
            }
        }
    }
}
