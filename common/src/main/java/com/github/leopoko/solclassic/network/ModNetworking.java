package com.github.leopoko.solclassic.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {
    public static final ResourceLocation SYNC_FOOD_HISTORY =
            ResourceLocation.fromNamespaceAndPath("solclassic", "sync_food_history");
    public static final ResourceLocation SYNC_CONFIG =
            ResourceLocation.fromNamespaceAndPath("solclassic", "sync_config");

    // メッセージの登録（Mod の初期化フェーズで呼び出す）
    // Architectury 13.0.8 の仕様に従い、サーバーとクライアントで登録方法を分ける:
    //   サーバー側: registerS2CPayloadType でタイプのみ登録（sendToPlayer に必要）
    //   クライアント側: registerReceiver で Fabric の registerS2C (@Environment(CLIENT)) を安全に呼び出す
    @SuppressWarnings("deprecation")
    public static void registerPackets() {
        if (Platform.getEnvironment() == Env.SERVER) {
            // 専用サーバー: S2C ペイロードタイプを登録して sendToPlayer() を有効にする
            // registerS2C は @Environment(EnvType.CLIENT) のため専用サーバーでは呼べない
            NetworkManager.registerS2CPayloadType(SYNC_FOOD_HISTORY);
            NetworkManager.registerS2CPayloadType(SYNC_CONFIG);
        } else {
            // クライアント: タイプとレシーバーをまとめて登録
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_FOOD_HISTORY,
                    (buf, context) -> {
                        SyncFoodHistoryPacket packet = new SyncFoodHistoryPacket(buf);
                        context.queue(() -> {
                            ClientPacketHandler.handleFoodHistoryPacket(packet.getFoodHistory());
                        });
                    });
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_CONFIG,
                    (buf, context) -> {
                        SyncConfigPacket packet = new SyncConfigPacket(buf);
                        context.queue(packet::applyToConfig);
                    });
        }
    }

    // サーバー -> クライアント送信用（プレイヤーに対して送信）
    public static void sendToPlayer(ServerPlayer player, SyncFoodHistoryPacket packet) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), player.registryAccess());
        packet.encode(buf);
        NetworkManager.sendToPlayer(player, SYNC_FOOD_HISTORY, buf);
    }

    // サーバー設定をクライアントへ同期する（ログイン時）
    @SuppressWarnings("deprecation")
    public static void sendConfigToPlayer(ServerPlayer player) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), player.registryAccess());
        new SyncConfigPacket().encode(buf);
        NetworkManager.sendToPlayer(player, SYNC_CONFIG, buf);
    }
}
