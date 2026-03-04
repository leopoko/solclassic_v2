package com.github.leopoko.solclassic.forge.mixin;

import com.github.leopoko.solclassic.forge.config.SolClassicConfigForge;
import com.github.leopoko.solclassic.forge.integration.ModCompatHelper;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.FoodHistorySync;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import com.github.leopoko.solclassic.utils.FoodDecayTracker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = Player.class, priority = 1100)
public class PlayerMixinForge {

    @Redirect(
            method = "eat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
                    remap = false),
            remap = true
    )
    private void modifyFoodRestoration(FoodData instance, Item item, ItemStack entity, LivingEntity arg) {
        Player player = (Player) (Object) this;

        String itemId = BuiltInRegistries.ITEM.getKey(entity.getItem()).toString();

        boolean isWickerBasket = false;
        if (itemId.equals("solclassic:wicker_basket")){
            isWickerBasket = true;
            WickerBasketItem wickerBasketItem = (WickerBasketItem) entity.getItem();
            entity = wickerBasketItem.getMostNutritiousFood(entity, player);
            // 実際の食べ物アイテムでitemIdを更新
            itemId = BuiltInRegistries.ITEM.getKey(entity.getItem()).toString();
        }

        for (String itemID_ : SolClassicConfigForge.CONFIG.foodBlacklist.get()) {
            if (itemId.equals(itemID_)) {
                FoodProperties foodProperties_ = entity.getItem().getFoodProperties();
                if (foodProperties_ == null) return;
                instance.eat(foodProperties_.getNutrition(), foodProperties_.getSaturationModifier());
                return;
            }
        }

        // shrink(1)が先に実行されるため、スタック数1の場合countが0になる。
        // getItem()はcount=0でも正しいアイテムを返すので、isEdible()で判定する。
        if (entity.getItem().isEdible()) {
            FoodProperties foodProperties = entity.getItem().getFoodProperties();
            if (foodProperties == null) return;

            // count=0のItemStackでも正しく比較できるが、履歴保存用にcount>=1のスタックを用意する
            ItemStack foodToRecord = entity.isEmpty() ? new ItemStack(entity.getItem()) : entity;

            float multiplier = FoodCalculator.CalculateMultiplier(foodToRecord, player);
            int nutrition = FoodCalculator.CalculateNutrition(foodProperties.getNutrition(), multiplier);

            // Diet MOD連携: 減衰倍率と実際の食べ物を保存（Dietのイベントハンドラで使用）
            FoodDecayTracker.recordDecay(player, entity, multiplier);

            instance.eat(nutrition, foodProperties.getSaturationModifier());

            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                // サーバー側で食事履歴を更新
                FoodHistoryHolder.INSTANCE.addFoodHistory((ServerPlayer) player, foodToRecord, SolClassicConfigForge.CONFIG.maxFoodHistorySize.get());
                FoodHistorySync.syncFoodHistory((ServerPlayer) player);

                // WickerBasketから食べた場合、実際の食べ物アイテムでイベントを発火し
                // Nutritional Balance等の食事イベント監視MODに通知する
                if (isWickerBasket) {
                    ModCompatHelper.notifyFoodConsumedFromBasket((ServerPlayer) player, entity);
                }
            }
        }
    }
}


