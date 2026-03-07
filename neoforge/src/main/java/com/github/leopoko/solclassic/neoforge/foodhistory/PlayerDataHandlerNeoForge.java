package com.github.leopoko.solclassic.neoforge.foodhistory;

import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.FoodHistorySync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.LinkedList;

@EventBusSubscriber()
public class PlayerDataHandlerNeoForge {
    // プレイヤーが再生成されるとき（例: 死亡やリスポーン）
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player originalPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        // 食事履歴を引き継ぐ
        LinkedList<ItemStack> originalHistory = FoodHistoryManagerNeoForge.loadFoodHistory(originalPlayer);
        FoodHistoryManagerNeoForge.saveFoodHistory(newPlayer, originalHistory);
    }

    // プレイヤーがログアウト/セーブ時にデータを保存
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        LinkedList<ItemStack> foodHistory = FoodHistoryHolder.INSTANCE.getFoodHistory((ServerPlayer) player);

        // 食事履歴をNBTに保存
        FoodHistoryManagerNeoForge.saveFoodHistory((ServerPlayer) player, foodHistory);
    }

    // プレイヤーがログイン時にデータを読み込む
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();

        // NBTから食事履歴を読み込む
        LinkedList<ItemStack> foodHistory = FoodHistoryManagerNeoForge.loadFoodHistory(player);
        FoodHistoryHolder.INSTANCE.setFoodHistory(player, foodHistory);

        // サーバーからクライアントにパケットを送信して履歴を同期する
        FoodHistorySync.syncFoodHistory(player);
    }

    // プレイヤーが食べ物を食べ終わった際に同期する
    @SubscribeEvent
    public static void onPlayerEatFood(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // クライアントサイドであれば処理を中断
            if (player.level().isClientSide()) {
                return;
            }

            // クライアントに食事履歴を同期
            FoodHistorySync.syncFoodHistory(player);
        }
    }
}
