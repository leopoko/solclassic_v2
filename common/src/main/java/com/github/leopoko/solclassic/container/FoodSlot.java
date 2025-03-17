package com.github.leopoko.solclassic.container;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FoodSlot extends Slot {

    public FoodSlot(Container container, int i, int j, int k) {
        super(container, i, j, k);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // スタックが食料かどうかをチェック
        boolean isEdible = stack.getItem().isEdible();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());


        if (itemId.toString().equals("solclassic:wicker_basket")) {
            isEdible = false;
        }
        return isEdible;
    }
}
