package com.github.leopoko.solclassic.network;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public class SyncFoodHistoryPacket {

    private final LinkedList<ItemStack> foodHistory;

    // デコード用コンストラクタ
    public SyncFoodHistoryPacket(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        LinkedList<ItemStack> list = new LinkedList<>();
        RegistryAccess registries = buf.registryAccess();
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                list.add(ItemStack.parseOptional(registries, tag));
            }
        }
        this.foodHistory = list;
    }

    // メッセージ作成用コンストラクタ
    public SyncFoodHistoryPacket(LinkedList<ItemStack> foodHistory) {
        this.foodHistory = foodHistory;
    }

    // エンコード：NBT形式でItemStackをシリアライズ
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.foodHistory.size());
        RegistryAccess registries = buf.registryAccess();
        for (ItemStack stack : this.foodHistory) {
            buf.writeNbt((CompoundTag) stack.saveOptional(registries));
        }
    }

    public LinkedList<ItemStack> getFoodHistory() {
        return foodHistory;
    }
}
