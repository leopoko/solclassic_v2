package com.github.leopoko.solclassic.fabric.foodhistory;

import com.github.leopoko.solclassic.utils.FoodHistory;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.ladysnake.cca.api.v3.component.Component;

public class FoodHistoryComponentFabric implements IFoodHistoryComponentFabric {
    private static final String FOOD_HISTORY_TAG = "FoodHistory";
    private final FoodHistory history = new FoodHistory();

    /**
     * 現在の食事履歴を返します。
     */
    public FoodHistory getHistory() {
        return history;
    }

    /**
     * FoodEventHandler などから渡された履歴で、内部の履歴を更新します。
     * 渡された各 ItemStack をコピーして設定するため、
     * 外部で変更されても内部データが影響を受けないようにします。
     *
     * @param newHistory 新しい食事履歴
     */
    public void setFood(FoodHistory newHistory) {
        // 内部の history 自身を渡された場合、clear() で履歴が全消去されてしまうため何もしない
        if (newHistory == null || newHistory == history) {
            return;
        }
        history.clear();
        // add() 経由で追加することで、消費回数キャッシュ (amountConsumed) も同時に再構築される
        for (ItemStack stack : newHistory.consumedItems) {
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
        for (ItemStack stack : history.consumedItems) {
            listTag.add((CompoundTag) stack.save(registryLookup));
        }
        tag.put(FOOD_HISTORY_TAG, listTag);
    }
}
