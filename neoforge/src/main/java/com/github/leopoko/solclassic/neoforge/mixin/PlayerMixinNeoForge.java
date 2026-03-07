package com.github.leopoko.solclassic.neoforge.mixin;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.FoodHistorySync;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import com.github.leopoko.solclassic.utils.FoodDecayTracker;
import com.github.leopoko.solclassic.neoforge.integration.ModCompatHelperNeoForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = 1100)
public class PlayerMixinNeoForge {

    @Unique
    private ItemStack solclassic$eatingStack = ItemStack.EMPTY;

    @Inject(method = "eat", at = @At("HEAD"))
    private void captureEatingItem(Level level, ItemStack stack, FoodProperties foodProperties, CallbackInfoReturnable<ItemStack> cir) {
        this.solclassic$eatingStack = stack;
    }

    @Redirect(
            method = "eat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V")
    )
    private void modifyFoodRestoration(FoodData instance, FoodProperties originalProps) {
        Player player = (Player) (Object) this;
        ItemStack itemStack = this.solclassic$eatingStack;

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

        boolean isWickerBasket = false;
        if (itemId.equals("solclassic:wicker_basket")) {
            isWickerBasket = true;
            WickerBasketItem wickerBasketItem = (WickerBasketItem) itemStack.getItem();
            itemStack = wickerBasketItem.getMostNutritiousFood(itemStack, player);
            itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        } else {
            ItemStack useItem = player.getUseItem();
            if (!useItem.isEmpty() && useItem.getItem() instanceof WickerBasketItem) {
                isWickerBasket = true;
            }
        }

        for (String itemID_ : SolclassicConfigData.foodBlacklist) {
            if (itemId.equals(itemID_)) {
                FoodProperties foodProperties_ = itemStack.get(DataComponents.FOOD);
                if (foodProperties_ == null) {
                    instance.eat(originalProps);
                    return;
                }
                instance.eat(foodProperties_);
                return;
            }
        }

        FoodProperties foodProperties = itemStack.get(DataComponents.FOOD);
        if (foodProperties == null) {
            foodProperties = originalProps;
        }

        ItemStack foodToRecord = itemStack.isEmpty() ? new ItemStack(itemStack.getItem()) : itemStack;

        float multiplier = FoodCalculator.CalculateMultiplier(foodToRecord, player);
        int nutrition = FoodCalculator.CalculateNutrition(foodProperties.nutrition(), multiplier);

        FoodDecayTracker.recordDecay(player, itemStack, multiplier);

        // foodProperties.saturation() は絶対値 (nutrition * modifier * 2.0f) なので
        // 元の saturation modifier を逆算する
        float saturationModifier = (foodProperties.nutrition() > 0)
                ? foodProperties.saturation() / ((float) foodProperties.nutrition() * 2.0f)
                : 0f;

        // 減衰後のnutritionで新しいFoodPropertiesを作成してeat()に渡す
        FoodProperties modifiedProps = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturationModifier)
                .build();
        instance.eat(modifiedProps);

        if (player instanceof ServerPlayer serverPlayer) {
            FoodHistoryHolder.INSTANCE.addFoodHistory(serverPlayer, foodToRecord, SolclassicConfigData.maxFoodHistorySize);
            FoodHistorySync.syncFoodHistory(serverPlayer);

            // WickerBasketから食べた場合、実際の食べ物アイテムでイベントを発火し
            // Nutritional Balance等の食事イベント監視MODに通知する
            if (isWickerBasket) {
                ModCompatHelperNeoForge.notifyFoodConsumedFromBasket(serverPlayer, foodToRecord);
            }
        }
    }
}
