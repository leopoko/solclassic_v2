package com.github.leopoko.solclassic.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;

/**
 * プレイヤーの食事履歴を保持するクラス。
 *
 * <p>食べた順の {@link ItemStack} キュー ({@code consumedItems}) に加えて、
 * アイテムごとの出現回数キャッシュ ({@code amountConsumed}) を持つ。
 * これにより長期減衰で使う総出現回数の取得が O(n) から O(1) になる。</p>
 *
 * <p><b>注意:</b> 両者の整合性を保つため、要素の追加・削除は必ず
 * {@link #add(ItemStack)} / {@link #add(ItemStack, int)} / {@link #clear()} を経由すること。
 * {@code consumedItems} を直接変更するとキャッシュがズレる。</p>
 */
public final class FoodHistory {
    private static final Logger LOGGER = Logger.getLogger("SolClassic");

    /** アイテムごとの累計消費回数（consumedItems の内容と常に一致させる） */
    public final Map<Item, Integer> amountConsumed;
    /** 食べた順に並んだ食事履歴（先頭が最古） */
    public final LinkedList<ItemStack> consumedItems;

    public FoodHistory() {
        amountConsumed = new HashMap<>();
        consumedItems = new LinkedList<>();
    }

    public void clear() {
        amountConsumed.clear();
        consumedItems.clear();
    }

    /**
     * 履歴にアイテムを追加し、上限を超えた分を古い順に切り捨てます。
     *
     * @param item       追加するアイテム
     * @param maxHistory 保持する履歴の最大件数
     */
    public void add(ItemStack item, int maxHistory) {
        if (maxHistory <= 0) {
            // 上限が 0 以下の場合は履歴を保持しない
            clear();
            return;
        }
        while (consumedItems.size() >= maxHistory) {
            decrementConsumption(consumedItems.removeFirst());
        }
        add(item);
    }

    /**
     * 履歴にアイテムを追加します（件数の上限は適用しません）。
     * NBT やパケットからの復元に使用します。
     */
    public void add(ItemStack item) {
        consumedItems.add(item);
        incrementConsumption(item);
    }

    /**
     * 履歴全体での対象アイテムの出現回数を返します。
     */
    public int getAmountConsumed(ItemStack target) {
        return amountConsumed.getOrDefault(target.getItem(), 0);
    }

    private void incrementConsumption(ItemStack item) {
        amountConsumed.merge(item.getItem(), 1, Integer::sum);
    }

    private void decrementConsumption(ItemStack item) {
        Integer previousAmountConsumed = amountConsumed.get(item.getItem());
        if (previousAmountConsumed == null) {
            // キューとキャッシュの不整合。ゲームを落とさずキャッシュを作り直して復旧する
            LOGGER.severe("食事履歴のキャッシュが不整合になりました。キャッシュを再構築します: " + this);
            rebuildCache();
            return;
        }
        if (previousAmountConsumed <= 1) {
            amountConsumed.remove(item.getItem());
        } else {
            amountConsumed.put(item.getItem(), previousAmountConsumed - 1);
        }
    }

    /** consumedItems の内容から amountConsumed を作り直します。 */
    private void rebuildCache() {
        amountConsumed.clear();
        for (ItemStack stack : consumedItems) {
            amountConsumed.merge(stack.getItem(), 1, Integer::sum);
        }
    }

    @Override
    public String toString() {
        StringJoiner map = new StringJoiner(", ", "{", "}");
        for (Map.Entry<Item, Integer> entry : amountConsumed.entrySet()) {
            map.add("\"" + BuiltInRegistries.ITEM.getKey(entry.getKey()) + "\": " + entry.getValue());
        }
        return "FoodHistory:{Map: %s, Queue: %s}".formatted(map, consumedItems);
    }
}
