package com.github.leopoko.solclassic.forge.mixin;

import com.github.leopoko.solclassic.item.WickerBasketItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Sophisticated Backpacks等の自動食事MODとのWickerBasket互換性対応。
 *
 * Forge固有の getFoodProperties(ItemStack, LivingEntity) をオーバーライドし、
 * WickerBasketの挙動をコンテキストに応じて切り替える:
 *
 * 1. プレイヤーインベントリ内: AppleSkin用のダミー値を返す（既存動作）
 * 2. SBバックパック等（インベントリ外）:
 *    - バスケット内に有効な食べ物がある場合: 高栄養値のFoodPropertiesを返し、
 *      SBのFeeding Upgradeに他の食べ物より優先して選択させる
 *    - バスケットが空 or すべて回復0%: nullを返して食べ物として認識させない
 * 3. entity==null: AppleSkin等の互換性のためダミー値を返す
 */
@Mixin(WickerBasketItem.class)
public abstract class WickerBasketMixinForge extends Item {

    public WickerBasketMixinForge(Properties properties) {
        super(properties);
    }

    /**
     * Forge固有の getFoodProperties(ItemStack, LivingEntity) をオーバーライド。
     * SBのFeeding Upgradeはこのメソッドで食べ物を判定・優先度を決定するため、
     * WickerBasketがバックパック内にある場合は高い栄養値を返して優先的に選択させる。
     */
    @Override
    public @Nullable FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        if (entity instanceof Player player) {
            // プレイヤーのインベントリ（メイン+オフハンド）内にある場合のみダミー値を返す
            // これによりAppleSkinのツールチップ表示は維持される
            for (ItemStack invStack : player.getInventory().items) {
                if (invStack == stack) return super.getFoodProperties(stack, entity);
            }
            for (ItemStack invStack : player.getInventory().offhand) {
                if (invStack == stack) return super.getFoodProperties(stack, entity);
            }

            // プレイヤーインベントリ外（SBバックパック等）
            // バスケット内に回復量が0より大きい食べ物があるかチェック
            ItemStack bestFood = WickerBasketItem.getMostNutritiousFood(stack, player);
            if (!bestFood.isEmpty()) {
                // 高い栄養値を返してSBのFeeding Upgradeに他の食べ物より優先させる
                // 実際の栄養計算はPlayerMixinForgeで行われる
                return new FoodProperties.Builder().nutrition(20).saturationMod(1.0f).build();
            }

            // バスケットが空 or すべての食べ物が回復0% → 食べ物として認識させない
            return null;
        }
        // entity==null の場合は保守的にダミー値を返す（AppleSkin等がnullで呼ぶ可能性）
        return super.getFoodProperties(stack, entity);
    }
}
