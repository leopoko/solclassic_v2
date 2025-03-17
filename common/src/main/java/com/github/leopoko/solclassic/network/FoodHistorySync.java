package com.github.leopoko.solclassic.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public class FoodHistorySync {
    // サーバー側で、FoodEventHandler 経由で取得した食事履歴をクライアントに送信する
    public static void syncFoodHistory(ServerPlayer player) {
        LinkedList<ItemStack> foodHistory = FoodHistoryHolder.INSTANCE.getFoodHistory(player);
        ModNetworking.sendToPlayer(player, new SyncFoodHistoryPacket(foodHistory));
    }
}