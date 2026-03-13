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
 * Sophisticated Backpacks等の自動食事MODがWickerBasketを食べ物として認識しないようにする。
 *
 * WickerBasketItemはAppleSkin連携のために isEdible()=true、getFoodProperties()=ダミー値 を返すが、
 * これによりSBのFeeding UpgradeがWickerBasketを自動食事の対象として検出してしまう。
 *
 * Forge固有の getFoodProperties(ItemStack, LivingEntity) をオーバーライドし、
 * プレイヤーのインベントリ内にあるWickerBasketのみダミー値を返す（AppleSkin用）。
 * それ以外（SBバックパック内等）はnullを返して食べ物として認識されないようにする。
 *
 * SBとのWickerBasket連携はSBFeedingUpgradeMixinがtryFeedingStackレベルで直接処理するため、
 * ここではWickerBasketをSBから隠すだけで良い。
 */
@Mixin(WickerBasketItem.class)
public abstract class WickerBasketMixinForge extends Item {

    public WickerBasketMixinForge(Properties properties) {
        super(properties);
    }

    /**
     * Forge固有の getFoodProperties(ItemStack, LivingEntity) をオーバーライド。
     * SBのFeeding Upgradeはこのメソッドで食べ物を判定するため、
     * WickerBasketがプレイヤーインベントリ外にある場合はnullを返して除外する。
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
            // プレイヤーインベントリ外（SBバックパック等）→ 食べ物として認識させない
            // SBとの連携はSBFeedingUpgradeMixinがtryFeedingStackレベルで直接処理する
            return null;
        }
        // entity==null の場合は保守的にダミー値を返す（AppleSkin等がnullで呼ぶ可能性）
        return super.getFoodProperties(stack, entity);
    }
}
