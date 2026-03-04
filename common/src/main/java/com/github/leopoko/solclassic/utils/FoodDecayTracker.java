package com.github.leopoko.solclassic.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Mixin→Diet連携イベントハンドラ間で減衰情報を受け渡すためのキャッシュ。
 * PlayerMixinで計算した減衰倍率と実際の食べ物を保存し、
 * DietIntegrationのイベントハンドラで取得・消費する。
 */
public class FoodDecayTracker {
    private static final Map<UUID, DecayInfo> cache = new ConcurrentHashMap<>();

    /**
     * 減衰情報を記録する。PlayerMixinの減衰計算後に呼び出す。
     */
    public static void recordDecay(Player player, ItemStack actualFood, float multiplier) {
        cache.put(player.getUUID(), new DecayInfo(actualFood.copy(), multiplier));
    }

    /**
     * 記録した減衰情報を取得し、キャッシュから削除する。
     * Dietイベントハンドラから呼び出す。
     * @return 減衰情報。未記録の場合はnull（ブラックリスト食品や空アイテムの場合）
     */
    public static DecayInfo getAndClear(Player player) {
        return cache.remove(player.getUUID());
    }

    public record DecayInfo(ItemStack actualFood, float multiplier) {}
}
