package com.github.leopoko.solclassic.forge.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LivingEntity.class, priority = 1100)
public class LivingEntityMixinForge {
    @Redirect(method = "eat", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void redirectShrink(ItemStack stack, int amount) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof Player) || !((Player)entity).getAbilities().instabuild) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            if (itemId.equals("solclassic:wicker_basket")) {
                // WickerBasketの場合はfinishUsingItem()で消費処理を行うため、
                // ここではWickerBasket自体のshrinkをスキップするだけ
            } else if (itemId.equals("phantasm:oblifruit")) {
                // Phantasm MODのoblifruitは40%の確率で消費
                if (Math.random() < 0.4) {
                    stack.shrink(1);
                }
            } else {
                stack.shrink(1);
            }
        }
    }

}
