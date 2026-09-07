package com.github.leopoko.solclassic.utils;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * バスケット（Basket / Wicker Basket）への収納を禁止するアイテムの判定。
 *
 * <p>設定 {@code basketBlacklist} に列挙されたアイテムIDは、バスケットに
 * 新規に入れることができず、Wicker Basket の自動選択対象からも除外される。</p>
 *
 * <p><b>注意:</b> 既にバスケット内にあるアイテムは取り出せる必要があるため、
 * NBT からの復元経路（{@code setItem}）ではこの判定を行わない。
 * 後からブラックリストに追加してもアイテムが消失しないようにするため。</p>
 *
 * <p>減衰の追跡対象から外す {@code foodBlacklist} とは目的が異なる別設定。</p>
 */
public final class BasketBlacklist {

    private BasketBlacklist() {
    }

    /**
     * 指定されたアイテムがバスケットへの収納を禁止されているかを返します。
     */
    public static boolean isBlacklisted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        List<String> blacklist = SolclassicConfigData.basketBlacklist;
        if (blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return blacklist.contains(itemId);
    }
}
