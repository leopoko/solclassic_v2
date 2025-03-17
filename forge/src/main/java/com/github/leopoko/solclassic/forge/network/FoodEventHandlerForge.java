package com.github.leopoko.solclassic.forge.network;

import com.github.leopoko.solclassic.network.IFoodEventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class FoodEventHandlerForge implements IFoodEventHandler {
    // プレイヤーごとの食事履歴を保存するマップ（キーはプレイヤーのUUID）
    private static final Map<UUID, LinkedList<ItemStack>> foodHistories = new WeakHashMap<>();

    /**
     * サーバー側で、指定されたプレイヤーの食事履歴を取得します。
     *
     * @param player サーバー側のプレイヤー
     * @return 食事履歴の LinkedList (存在しない場合は新規作成)
     */
    public LinkedList<ItemStack> getFoodHistory(ServerPlayer player) {
        return foodHistories.computeIfAbsent(player.getUUID(), uuid -> new LinkedList<>());
    }

    /**
     * サーバー側で、指定されたプレイヤーの食事履歴に新しいアイテムを追加します。
     * 履歴が最大件数を超える場合、先頭の（古い）エントリを削除します。
     *
     * @param player    サーバー側のプレイヤー
     * @param foodStack 追加する食事アイテムの ItemStack
     */
    public void addFoodHistory(ServerPlayer player, ItemStack foodStack, int maxHistory) {
        LinkedList<ItemStack> history = getFoodHistory(player);
        history.add(foodStack.copy()); // アイテムスタックはコピーして保存
        if (history.size() > maxHistory) {
            history.removeFirst();
        }
        // オプション：チャットにメッセージを表示
        //player.sendSystemMessage(Component.literal("食事履歴が更新されました"));
    }

    /**
     * クライアント側で、サーバーから送信された食事履歴をプレイヤーに設定します。
     * ここでは例として、チャットに更新完了のメッセージを表示しています。
     *
     * @param player      クライアント側のプレイヤー
     * @param foodHistory サーバーから送信された食事履歴
     */
    public void setFoodHistory(Player player, LinkedList<ItemStack> foodHistory) {
        foodHistories.put(player.getUUID(), foodHistory);
        // オプション：チャットにメッセージを表示
        //player.sendSystemMessage(Component.literal("食事履歴が更新されました"));
    }

    public void resetFoodHistory(Player player) {
        foodHistories.remove(player.getUUID());
    }

    /**
     * 指定されたプレイヤーの食事履歴から、対象の ItemStack と同じアイテムIDのものが何個記録されているかを返します。
     *
     * @param player    対象のプレイヤー（サーバー/クライアントどちらでも可）
     * @param target    対象の ItemStack（比較は getItem() で行う）
     * @return 食事履歴内に記録されている、対象アイテムの個数
     */
    public int countFoodEaten(Player player, ItemStack target) {
        LinkedList<ItemStack> history = foodHistories.get(player.getUUID());
        if (history == null) {
            return 0;
        }
        int count = 0;
        // 対象のアイテムと同じかどうかを getItem() で判定
        for (ItemStack stack : history) {
            if (stack.getItem().equals(target.getItem())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 指定されたプレイヤーの食事履歴の直近 n 件のエントリ中で、
     * 対象の ItemStack（同じアイテム）が何個記録されているかを返します。
     *
     * @param player 対象のプレイヤー（サーバーまたはクライアント）
     * @param target 対象の ItemStack（比較は getItem() を用いる）
     * @param n      直近 n 件の履歴に限定して検索する
     * @return 直近 n 件中に対象アイテムが出現した回数
     */
    public int countFoodEatenRecent(Player player, ItemStack target, int n) {
        LinkedList<ItemStack> history = foodHistories.get(player.getUUID());
        if (history == null || history.isEmpty()) {
            return 0;
        }
        int count = 0;
        int processed = 0;
        // 最新の履歴から逆順に n 件分だけ走査
        for (Iterator<ItemStack> iterator = history.descendingIterator(); iterator.hasNext() && processed < n; processed++) {
            ItemStack stack = iterator.next();
            if (stack.getItem().equals(target.getItem())) {
                count++;
            }
        }
        return count;
    }
}
