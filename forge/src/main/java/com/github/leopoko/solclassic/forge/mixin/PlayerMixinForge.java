package com.github.leopoko.solclassic.forge.mixin;

import com.github.leopoko.solclassic.forge.config.SolClassicConfigForge;
import com.github.leopoko.solclassic.forge.config.SolClassicConfigInitForge;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.utils.FoodCalculator;
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

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Player.class, priority = 1100)
public class PlayerMixinForge {

    @Redirect(
            method = "eat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V"),
            remap = true
    )
    private void modifyFoodRestoration(FoodData instance, Item item, ItemStack entity, LivingEntity arg) {
        Player player = (Player) (Object) this;

        String itemId = BuiltInRegistries.ITEM.getKey(entity.getItem()).toString();

        if (itemId.equals("solclassic:wicker_basket")){
            WickerBasketItem wickerBasketItem = (WickerBasketItem) entity.getItem();
            entity = wickerBasketItem.getMostNutritiousFood(entity, player);
            // Update itemId after retrieving the actual food item
            itemId = BuiltInRegistries.ITEM.getKey(entity.getItem()).toString();
        }

        for (String itemID_ : SolClassicConfigForge.CONFIG.foodBlacklist.get()) {
            if (itemId.equals(itemID_)) {
                FoodProperties foodProperties_ = entity.getItem().getFoodProperties();
                instance.eat(foodProperties_.getNutrition(), foodProperties_.getSaturationModifier());
                return;
            }
        }

        SolClassicConfigInitForge.init();

        if (!entity.isEmpty()) {
            FoodProperties foodProperties = entity.getItem().getFoodProperties();

            float multiplier = FoodCalculator.CalculateMultiplier(entity, player);
            int nutrition = FoodCalculator.CalculateNutrition(foodProperties.getNutrition(), multiplier);

            instance.eat(nutrition, foodProperties.getSaturationModifier());

            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                // サーバー側で食事履歴を更新
                FoodHistoryHolder.INSTANCE.addFoodHistory((ServerPlayer) player, entity, SolClassicConfigForge.CONFIG.maxFoodHistorySize.get());
            }
        }
    }
}


