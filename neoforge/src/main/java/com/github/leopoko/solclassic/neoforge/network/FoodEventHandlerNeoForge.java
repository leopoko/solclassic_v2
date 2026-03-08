package com.github.leopoko.solclassic.neoforge.network;

import com.github.leopoko.solclassic.network.IFoodEventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FoodEventHandlerNeoForge implements IFoodEventHandler {
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
    }

    /**
     * クライアント側で、サーバーから送信された食事履歴をプレイヤーに設定します。
     *
     * @param player      クライアント側のプレイヤー
     * @param foodHistory サーバーから送信された食事履歴
     */
    public void setFoodHistory(Player player, LinkedList<ItemStack> foodHistory) {
        foodHistories.put(player.getUUID(), foodHistory);
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

    @Override
    public LinkedList<ItemStack> getClientFoodHistory(Player player) {
        LinkedList<ItemStack> history = foodHistories.get(player.getUUID());
        return history != null ? history : new LinkedList<>();
    }

    /**
     * NeoForge拡張APIを使用して、Quality Food等のMODによる品質修正を反映した栄養値を返す。
     * Item.getFoodProperties(ItemStack, LivingEntity) はNeoForge固有のメソッドで、
     * MODがこのメソッドをオーバーライドすることでアイテムごとに異なるFoodPropertiesを返すことができる。
     */
    @Override
    public int getEffectiveNutrition(ItemStack stack, @Nullable Player player) {
        FoodProperties fp = stack.getItem().getFoodProperties(stack, (LivingEntity) player);
        if (fp == null) fp = stack.get(DataComponents.FOOD);
        return fp != null ? fp.nutrition() : 0;
    }

    @Override
    public float getEffectiveSaturationModifier(ItemStack stack, @Nullable Player player) {
        FoodProperties fp = stack.getItem().getFoodProperties(stack, (LivingEntity) player);
        if (fp == null) fp = stack.get(DataComponents.FOOD);
        if (fp == null) return 0f;
        // saturation() は絶対値 (nutrition * modifier * 2.0f) なので modifier を逆算
        return (fp.nutrition() > 0)
                ? fp.saturation() / ((float) fp.nutrition() * 2.0f)
                : 0f;
    }
}
