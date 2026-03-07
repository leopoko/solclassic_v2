package com.github.leopoko.solclassic.fabric.foodhistory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.ladysnake.cca.api.v3.component.Component;

import java.util.LinkedList;

public class FoodHistoryComponentFabric implements IFoodHistoryComponentFabric {
    private static final String FOOD_HISTORY_TAG = "FoodHistory";
    private final LinkedList<ItemStack> history = new LinkedList<>();

    public LinkedList<ItemStack> getHistory() {
        return history;
    }

    public void setFood(LinkedList<ItemStack> newHistory) {
        LinkedList<ItemStack> copyHistory = new LinkedList<>(newHistory);
        history.clear();
        for (ItemStack stack : copyHistory) {
            history.add(stack.copy());
        }
    }

    public void addFood(ItemStack stack) {
        history.add(stack.copy());
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        history.clear();
        if (tag.contains(FOOD_HISTORY_TAG, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(FOOD_HISTORY_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag stackTag = listTag.getCompound(i);
                history.add(ItemStack.parseOptional(registryLookup, stackTag));
            }
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        ListTag listTag = new ListTag();
        for (ItemStack stack : history) {
            listTag.add((CompoundTag) stack.save(registryLookup));
        }
        tag.put(FOOD_HISTORY_TAG, listTag);
    }
}
