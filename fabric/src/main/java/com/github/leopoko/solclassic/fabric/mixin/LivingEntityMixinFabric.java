package com.github.leopoko.solclassic.fabric.mixin;

import com.github.leopoko.solclassic.item.WickerBasketItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityMixinFabric {
    @Redirect(method = "eat", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void redirectShrink(ItemStack stack, int amount) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof Player) || !((Player)entity).getAbilities().instabuild) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            if (!itemId.equals("solclassic:wicker_basket")) {
                stack.shrink(1);
            }
            else {
                if (stack.getItem() instanceof WickerBasketItem) {
                    ItemStack mostNutritiousItem = WickerBasketItem.getMostNutritiousFood(stack, (Player)entity);
                    WickerBasketItem.shrinkMostNutritiousItemFromInventory(stack, (Player)entity);

                    if (!mostNutritiousItem.is(ItemStack.EMPTY.getItem())) {

                        if (mostNutritiousItem.getItem().getCraftingRemainingItem() != null) {
                            ItemStack containerItem = mostNutritiousItem.getItem().getCraftingRemainingItem().getDefaultInstance();
                            if (!((Player)entity).getInventory().add(containerItem)) {
                                ((Player)entity).drop(containerItem, false);
                            }
                        }

                        if (mostNutritiousItem.getItem().getFoodProperties().getEffects() != null) {
                            for (var effect : mostNutritiousItem.getItem().getFoodProperties().getEffects()) {
                                entity.addEffect(effect.getFirst());
                            }
                        }
                    }
                }
            }
        }
    }

}
