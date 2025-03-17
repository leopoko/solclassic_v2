package com.github.leopoko.solclassic.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public class ClientPacketHandler {
    // クライアント側で受信した食事履歴をプレイヤーに反映する
    public static void handleFoodHistoryPacket(LinkedList<ItemStack> foodHistory) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            FoodHistoryHolder.INSTANCE.setFoodHistory(player, foodHistory);
        }
    }
}