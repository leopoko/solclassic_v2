package com.github.leopoko.solclassic.network;

import com.github.leopoko.solclassic.utils.FoodHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientPacketHandler {
    // クライアント側で受信した食事履歴をプレイヤーに反映する
    public static void handleFoodHistoryPacket(FoodHistory foodHistory) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            FoodHistoryHolder.INSTANCE.setFoodHistory(player, foodHistory);
        }
    }
}