package com.github.leopoko.solclassic.fabric.mixin;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.FoodHistorySync;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import com.github.leopoko.solclassic.utils.FoodDecayTracker;
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

@Mixin(Player.class)
public class PlayerMixinFabric {

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
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"),
            remap = true
    )
    private void modifyFoodRestoration(FoodData instance, FoodProperties originalProps) {
        Player player = (Player) (Object) this;
        ItemStack itemStack = this.solclassic$eatingStack;

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

        if (itemId.equals("solclassic:wicker_basket")) {
            WickerBasketItem wickerBasketItem = (WickerBasketItem) itemStack.getItem();
            itemStack = wickerBasketItem.getMostNutritiousFood(itemStack, player);
            itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        } else {
            ItemStack useItem = player.getUseItem();
            if (!useItem.isEmpty() && useItem.getItem() instanceof WickerBasketItem) {
                // 委譲方式: itemStackは既に実際の食べ物
            }
        }

        for (String itemID_ : SolclassicConfigData.foodBlacklist) {
            if (itemId.equals(itemID_)) {
                // ブラックリスト食品: 減衰なしで栄養値をそのまま適用（Quality Food等の品質修正は反映）
                int nutrition_ = FoodHistoryHolder.INSTANCE.getEffectiveNutrition(itemStack, player);
                float saturationModifier_ = FoodHistoryHolder.INSTANCE.getEffectiveSaturationModifier(itemStack, player);
                if (nutrition_ == 0 && saturationModifier_ == 0f) {
                    instance.eat(originalProps);
                    return;
                }
                FoodProperties blacklistProps = new FoodProperties.Builder()
                        .nutrition(nutrition_)
                        .saturationModifier(saturationModifier_)
                        .build();
                instance.eat(blacklistProps);
                return;
            }
        }

        // Quality Food等のMODによる品質修正を反映した栄養値を取得
        int baseNutrition = FoodHistoryHolder.INSTANCE.getEffectiveNutrition(itemStack, player);
        float baseSaturationModifier = FoodHistoryHolder.INSTANCE.getEffectiveSaturationModifier(itemStack, player);
        if (baseNutrition == 0 && baseSaturationModifier == 0f) {
            instance.eat(originalProps);
            return;
        }

        ItemStack foodToRecord = itemStack.isEmpty() ? new ItemStack(itemStack.getItem()) : itemStack;

        float multiplier = FoodCalculator.CalculateMultiplier(foodToRecord, player);
        int nutrition = FoodCalculator.CalculateNutrition(baseNutrition, multiplier);

        FoodDecayTracker.recordDecay(player, itemStack, multiplier);

        // 減衰後のnutritionで新しいFoodPropertiesを作成してeat()に渡す
        FoodProperties modifiedProps = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(baseSaturationModifier)
                .build();
        instance.eat(modifiedProps);

        if (player instanceof ServerPlayer serverPlayer) {
            FoodHistoryHolder.INSTANCE.addFoodHistory(serverPlayer, foodToRecord, SolclassicConfigData.maxFoodHistorySize);
            FoodHistorySync.syncFoodHistory(serverPlayer);
        }
    }
}
