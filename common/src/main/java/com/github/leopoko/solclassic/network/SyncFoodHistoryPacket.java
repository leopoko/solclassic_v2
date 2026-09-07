package com.github.leopoko.solclassic.network;

import com.github.leopoko.solclassic.utils.FoodHistory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class SyncFoodHistoryPacket {

    private final FoodHistory foodHistory;

    // デコード用コンストラクタ
    public SyncFoodHistoryPacket(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        FoodHistory history = new FoodHistory();
        RegistryAccess registries = buf.registryAccess();
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                // add(ItemStack) 経由で入れることで消費回数キャッシュも同時に構築される
                history.add(ItemStack.parseOptional(registries, tag));
            }
        }
        this.foodHistory = history;
    }

    // メッセージ作成用コンストラクタ
    public SyncFoodHistoryPacket(FoodHistory foodHistory) {
        this.foodHistory = foodHistory;
    }

    // エンコード：NBT形式でItemStackをシリアライズ
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.foodHistory.consumedItems.size());
        RegistryAccess registries = buf.registryAccess();
        for (ItemStack stack : this.foodHistory.consumedItems) {
            buf.writeNbt((CompoundTag) stack.saveOptional(registries));
        }
    }

    public FoodHistory getFoodHistory() {
        return foodHistory;
    }
}
