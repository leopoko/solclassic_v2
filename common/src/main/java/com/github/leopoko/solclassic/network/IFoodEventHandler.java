package com.github.leopoko.solclassic.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public interface IFoodEventHandler {

    /**
     * サーバー側で、指定されたプレイヤーの食事履歴を取得します。
     *
     * @param player サーバー側のプレイヤー
     * @return 食事履歴の LinkedList (存在しない場合は新規作成)
     */
    public LinkedList<ItemStack> getFoodHistory(ServerPlayer player);

    /**
     * サーバー側で、指定されたプレイヤーの食事履歴に新しいアイテムを追加します。
     * 履歴が最大件数を超える場合、先頭の（古い）エントリを削除します。
     *
     * @param player    サーバー側のプレイヤー
     * @param foodStack 追加する食事アイテムの ItemStack
     */
    public void addFoodHistory(ServerPlayer player, ItemStack foodStack, int maxHistory);

    /**
     * クライアント側で、サーバーから送信された食事履歴をプレイヤーに設定します。
     * ここでは例として、チャットに更新完了のメッセージを表示しています。
     *
     * @param player      クライアント側のプレイヤー
     * @param foodHistory サーバーから送信された食事履歴
     */
    public void setFoodHistory(Player player, LinkedList<ItemStack> foodHistory);

    /**
     * サーバー側で、指定されたプレイヤーの食事履歴をリセットします。
     *
     * @param player サーバー側のプレイヤー
     */
    public void resetFoodHistory(Player player);

    /**
     * 指定されたプレイヤーの食事履歴から、対象の ItemStack と同じアイテムIDのものが何個記録されているかを返します。
     *
     * @param player    対象のプレイヤー（サーバー/クライアントどちらでも可）
     * @param target    対象の ItemStack（比較は getItem() で行う）
     * @return 食事履歴内に記録されている、対象アイテムの個数
     */
    public int countFoodEaten(Player player, ItemStack target);

    /**
     * 指定されたプレイヤーの食事履歴の直近 n 件のエントリ中で、
     * 対象の ItemStack（同じアイテム）が何個記録されているかを返します。
     *
     * @param player 対象のプレイヤー（サーバーまたはクライアント）
     * @param target 対象の ItemStack（比較は getItem() を用いる）
     * @param n      直近 n 件の履歴に限定して検索する
     * @return 直近 n 件中に対象アイテムが出現した回数
     */
    public int countFoodEatenRecent(Player player, ItemStack target, int n);
}
