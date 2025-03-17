package com.github.leopoko.solclassic.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.function.Supplier;

public class SyncFoodHistoryPacket {

    private final LinkedList<ItemStack> foodHistory;

    // デコード用コンストラクタ
    public SyncFoodHistoryPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        LinkedList<ItemStack> list = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            list.add(buf.readItem());
        }
        this.foodHistory = list;
    }

    // メッセージ作成用コンストラクタ
    public SyncFoodHistoryPacket(LinkedList<ItemStack> foodHistory) {
        this.foodHistory = foodHistory;
    }

    // エンコード：書き出し順に合わせてデータをバッファに書き込む
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.foodHistory.size());
        for (ItemStack stack : this.foodHistory) {
            buf.writeItem(stack);
        }
    }

    // 受信時の処理（apply メソッド内でクライアント側のみ実行）
    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (Minecraft.getInstance() != null) {
                ClientPacketHandler.handleFoodHistoryPacket(this.foodHistory);
            }
        });
    }

    // 必要に応じて getter も用意できます
    public LinkedList<ItemStack> getFoodHistory() {
        return foodHistory;
    }
}
