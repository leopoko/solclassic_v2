package com.github.leopoko.solclassic.network;

import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {
    // チャンネル作成：ResourceLocation の第1引数は modid に置き換えてください
    public static final NetworkChannel CHANNEL = NetworkChannel.create(new ResourceLocation("solclassic", "networking_channel"));

    // メッセージの登録（Mod の初期化フェーズで呼び出す）
    public static void registerPackets() {
        CHANNEL.register(SyncFoodHistoryPacket.class,
                SyncFoodHistoryPacket::encode,
                SyncFoodHistoryPacket::new,
                SyncFoodHistoryPacket::apply);
    }

    // サーバー -> クライアント送信用（プレイヤーに対して送信）
    public static void sendToPlayer(ServerPlayer player, SyncFoodHistoryPacket packet) {
        CHANNEL.sendToPlayer(player, packet);
    }

    // クライアント -> サーバー送信用（必要なら）
    public static void sendToServer(SyncFoodHistoryPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
