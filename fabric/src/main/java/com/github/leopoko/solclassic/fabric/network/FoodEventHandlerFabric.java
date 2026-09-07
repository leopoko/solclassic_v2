package com.github.leopoko.solclassic.fabric.network;

import com.github.leopoko.solclassic.fabric.foodhistory.IFoodHistoryComponentFabric;
import com.github.leopoko.solclassic.network.IFoodEventHandler;
import com.github.leopoko.solclassic.utils.FoodHistory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

import static com.github.leopoko.solclassic.fabric.foodhistory.FoodHistoryComponentImplFabric.FOOD_HISTORY;

public class FoodEventHandlerFabric  implements IFoodEventHandler {
    // プレイヤーごとの食事履歴を保存するマップ（キーはプレイヤーのUUID）
    private static final Map<UUID, FoodHistory> foodHistories = new WeakHashMap<>();

    /**
     * サーバー側で、指定されたプレイヤーの食事履歴を取得します。
     *
     * @param player サーバー側のプレイヤー
     * @return 食事履歴 (存在しない場合は新規作成)
     */
    public FoodHistory getFoodHistory(ServerPlayer player) {
        return FOOD_HISTORY.get(player).getHistory();
    }

    /**
     * サーバー側で、指定されたプレイヤーの食事履歴に新しいアイテムを追加します。
     * 履歴が最大件数を超える場合、先頭の（古い）エントリを削除します。
     *
     * @param player    サーバー側のプレイヤー
     * @param foodStack 追加する食事アイテムの ItemStack
     */
    public void addFoodHistory(ServerPlayer player, ItemStack foodStack, int maxHistory) {
        // getHistory() が返すのはコンポーネント内部の実体なので、その場で更新すればよい。
        // setFood() に自分自身を渡すと自己代入になるため呼ばない。
        FoodHistory history = getFoodHistory(player);
        history.add(foodStack.copy(), maxHistory); // アイテムスタックはコピーして保存
    }

    /**
     * クライアント側で、サーバーから送信された食事履歴をプレイヤーに設定します。
     *
     * @param player      クライアント側のプレイヤー
     * @param foodHistory サーバーから送信された食事履歴
     */
    public void setFoodHistory(Player player, FoodHistory foodHistory) {
        foodHistories.put(player.getUUID(), foodHistory);
        FOOD_HISTORY.get(player).setFood(foodHistory);
    }

    public void resetFoodHistory(Player player) {
        foodHistories.remove(player.getUUID());
        FOOD_HISTORY.get(player).setFood(new FoodHistory());
    }

    /**
     * 指定されたプレイヤーの食事履歴から、対象の ItemStack と同じアイテムIDのものが何個記録されているかを返します。
     *
     * @param player    対象のプレイヤー（サーバー/クライアントどちらでも可）
     * @param target    対象の ItemStack（比較は getItem() で行う）
     * @return 食事履歴内に記録されている、対象アイテムの個数
     */
    public int countFoodEaten(Player player, ItemStack target) {
        if (player == null) {
            return 0;
        }
        FoodHistory history = FOOD_HISTORY.get(player).getHistory();
        if (history == null) {
            return 0;
        }
        // 消費回数キャッシュから O(1) で取得する
        return history.getAmountConsumed(target);
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
        if (player == null) {
            return 0;
        }
        // consumedItems を参照する前に FoodHistory 自体の null を判定する
        FoodHistory history = FOOD_HISTORY.get(player).getHistory();
        if (history == null || history.consumedItems.isEmpty()) {
            return 0;
        }
        int count = 0;
        int processed = 0;
        // 最新の履歴から逆順に n 件分だけ走査
        for (Iterator<ItemStack> iterator = history.consumedItems.descendingIterator(); iterator.hasNext() && processed < n; processed++) {
            ItemStack stack = iterator.next();
            if (stack.getItem().equals(target.getItem())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public FoodHistory getClientFoodHistory(Player player) {
        if (player == null) {
            return new FoodHistory();
        }
        FoodHistory history = FOOD_HISTORY.get(player).getHistory();
        return history != null ? history : new FoodHistory();
    }
}
