package com.github.leopoko.solclassic.fabric.mixin;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.FoodHistorySync;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class PlayerMixinFabric {

    @Redirect(
            method = "eat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V"),
            remap = true
    )
    private void modifyFoodRestoration(FoodData instance, Item item, ItemStack itemStack) {
        Player player = (Player) (Object) this;

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

        if (itemId.equals("solclassic:wicker_basket")) {
            WickerBasketItem wickerBasketItem = (WickerBasketItem) itemStack.getItem();
            itemStack = wickerBasketItem.getMostNutritiousFood(itemStack, player);
            // Update itemId to the actual food item after extraction
            itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        }

        for (String itemID_ : SolclassicConfigData.foodBlacklist) {
            if (itemId.equals(itemID_)) {
                FoodProperties foodProperties_ = itemStack.getItem().getFoodProperties();
                if (foodProperties_ == null) return;
                instance.eat(foodProperties_.getNutrition(), foodProperties_.getSaturationModifier());
                return;
            }
        }

        // shrink(1)が先に実行されるため、スタック数1の場合countが0になる。
        // getItem()はcount=0でも正しいアイテムを返すので、isEdible()で判定する。
        if (itemStack.getItem().isEdible()) {
            FoodProperties foodProperties = itemStack.getItem().getFoodProperties();
            if (foodProperties == null) return;

            // count=0のItemStackでも正しく比較できるが、履歴保存用にcount>=1のスタックを用意する
            ItemStack foodToRecord = itemStack.isEmpty() ? new ItemStack(itemStack.getItem()) : itemStack;

            float multiplier = FoodCalculator.CalculateMultiplier(foodToRecord, player);
            int nutrition = FoodCalculator.CalculateNutrition(foodProperties.getNutrition(), multiplier);

            instance.eat(nutrition, foodProperties.getSaturationModifier());

            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                FoodHistoryHolder.INSTANCE.addFoodHistory((ServerPlayer) player, foodToRecord, SolclassicConfigData.maxFoodHistorySize);
                FoodHistorySync.syncFoodHistory((ServerPlayer) player);
            }
        }
    }
}


