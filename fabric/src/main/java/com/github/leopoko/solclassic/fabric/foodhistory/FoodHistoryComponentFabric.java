package com.github.leopoko.solclassic.fabric.foodhistory;

import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public class FoodHistoryComponentFabric implements IFoodHistoryComponentFabric {
    private static final String FOOD_HISTORY_TAG = "FoodHistory";
    private final LinkedList<ItemStack> history = new LinkedList<>();

    /**
     * 現在の食事履歴を返します。
     */
    public LinkedList<ItemStack> getHistory() {
        return history;
    }

    /**
     * FoodEventHandler などから渡された履歴で、内部の履歴を更新します。
     * 渡されたリストの各 ItemStack をコピーして設定するため、
     * 外部で変更されても内部データが影響を受けないようにします。
     *
     * @param newHistory 新しい食事履歴
     */
    public void setFood(LinkedList<ItemStack> newHistory) {
        LinkedList<ItemStack> copyHistory = new LinkedList<>(newHistory);
        history.clear();
        for (ItemStack stack : copyHistory) {
            history.add(stack.copy());
        }
    }

    /**
     * （参考用）個別に食事アイテムを追加するメソッド
     */
    public void addFood(ItemStack stack) {
        history.add(stack.copy());
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        System.out.println("Food history loaded (" + history.size() + " entries):");
        history.clear();
        if (tag.contains(FOOD_HISTORY_TAG, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(FOOD_HISTORY_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag stackTag = listTag.getCompound(i);
                history.add(ItemStack.of(stackTag));
            }
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (ItemStack stack : history) {
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            listTag.add(stackTag);
        }
        tag.put(FOOD_HISTORY_TAG, listTag);
    }
}