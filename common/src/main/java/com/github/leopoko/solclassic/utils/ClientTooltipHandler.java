package com.github.leopoko.solclassic.utils;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ClientTooltipHandler {
    public static void init() {
        ClientTooltipEvent.ITEM.register((ItemStack stack, List<Component> tooltips, TooltipFlag flag) -> {
            if (stack.getItem().isEdible()) {
                FoodProperties foodProps = stack.getItem().getFoodProperties();
                if (foodProps != null) {

                    if (Minecraft.getInstance().player != null) {
                        Player player = Minecraft.getInstance().player;
                        float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
                        multiplier = (1f - multiplier) * 100f;
                        String s = Integer.toString((int) multiplier);

                        if (!s.equals("0")) {
                            tooltips.add(Component.translatable("tooltip.food_reduction", s));
                        }
                    }
                }
            }
        });
    }
}
