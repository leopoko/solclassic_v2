package com.github.leopoko.solclassic.forge.foodhistory;

import com.github.leopoko.solclassic.utils.FoodHistory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


public class FoodHistoryManagerForge {
    private static final String FOOD_HISTORY_TAG = "FoodHistory";

    // プレイヤーに食事履歴を保存
    public static void saveFoodHistory(Player player, FoodHistory foodHistory) {

        CompoundTag playerData = player.getPersistentData();
        ListTag foodHistoryTag = new ListTag();

        // 食べ物履歴をNBTに変換して保存
        for (ItemStack stack : foodHistory.consumedItems) {
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);  // ItemStackをNBTに変換
            foodHistoryTag.add(stackTag);
        }

        // プレイヤーのNBTデータに保存
        playerData.put(FOOD_HISTORY_TAG, foodHistoryTag);
    }

    // プレイヤーから食事履歴を読み込む
    public static FoodHistory loadFoodHistory(Player player) {
        FoodHistory foodHistory = new FoodHistory();
        CompoundTag playerData = player.getPersistentData();

        // プレイヤーのデータから食事履歴を取得
        if (playerData.contains(FOOD_HISTORY_TAG)) {
            ListTag foodHistoryTag = playerData.getList(FOOD_HISTORY_TAG, Tag.TAG_COMPOUND);
            for (Tag tag : foodHistoryTag) {
                CompoundTag stackTag = (CompoundTag) tag;
                ItemStack stack = ItemStack.of(stackTag);  // NBTからItemStackに変換
                foodHistory.add(stack);
            }
        }

        return foodHistory;
    }
}
