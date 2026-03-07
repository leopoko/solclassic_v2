package com.github.leopoko.solclassic.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {
    public static final ResourceLocation SYNC_FOOD_HISTORY =
            ResourceLocation.fromNamespaceAndPath("solclassic", "sync_food_history");

    // メッセージの登録（Mod の初期化フェーズで呼び出す）
    public static void registerPackets() {
        // S2C レシーバー登録（ペイロードタイプも自動的に登録される）
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_FOOD_HISTORY,
                (buf, context) -> {
                    SyncFoodHistoryPacket packet = new SyncFoodHistoryPacket(buf);
                    context.queue(() -> {
                        ClientPacketHandler.handleFoodHistoryPacket(packet.getFoodHistory());
                    });
                });
    }

    // サーバー -> クライアント送信用（プレイヤーに対して送信）
    public static void sendToPlayer(ServerPlayer player, SyncFoodHistoryPacket packet) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), player.registryAccess());
        packet.encode(buf);
        NetworkManager.sendToPlayer(player, SYNC_FOOD_HISTORY, buf);
    }
}
