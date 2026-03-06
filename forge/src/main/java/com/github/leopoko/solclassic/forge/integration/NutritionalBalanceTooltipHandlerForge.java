package com.github.leopoko.solclassic.forge.integration;

import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Nutritional Balance MODツールチップ連携（Forge版・クライアント専用）。
 * - WickerBasket: 選択された食べ物のNBツールチップ（減衰倍率適用済み）を表示
 * - 通常食べ物: NBの品質値にSoL Classicの減衰倍率を適用
 *
 * NBツールチップ実際の形式（色コード付き）:
 *   §7Nutrients: §2Carbs§7 (5.0NU)§r
 * getString()で取得する文字列にも§コードが含まれるため、
 * ChatFormatting.stripFormatting()で除去してからラベル検索する。
 */
public class NutritionalBalanceTooltipHandlerForge {

    /** 再帰呼び出し防止フラグ（WickerBasketの食べ物ツールチップ取得時） */
    private static boolean isGettingFoodTooltip = false;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(NutritionalBalanceTooltipHandlerForge.class);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (isGettingFoodTooltip) return;

        Player player = event.getEntity();
        if (player == null) return;

        ItemStack stack = event.getItemStack();
        List<Component> tooltips = event.getToolTip();
        String nutrientsLabel = I18n.get("nutritionalbalance.nutrients");

        if (stack.getItem() instanceof WickerBasketItem) {
            replaceWithFoodNBLine(tooltips, nutrientsLabel, stack, player);
            return;
        }

        if (!stack.getItem().isEdible()) return;

        float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
        if (multiplier >= 1.0f) return;

        int idx = findNBLine(tooltips, nutrientsLabel);
        if (idx >= 0) {
            String raw = tooltips.get(idx).getString();
            String scaled = applyScale(raw, multiplier);
            if (!scaled.equals(raw)) {
                tooltips.set(idx, Component.nullToEmpty(scaled));
            }
        }
    }

    /**
     * WickerBasketのNBツールチップを、選択された食べ物のNBツールチップに差し替える。
     */
    private static void replaceWithFoodNBLine(List<Component> tooltips, String nutrientsLabel,
                                               ItemStack basketStack, Player player) {
        int basketIdx = findNBLine(tooltips, nutrientsLabel);
        if (basketIdx < 0) return;

        ItemStack food = WickerBasketItem.getMostNutritiousFood(basketStack, player);
        if (food.isEmpty()) {
            tooltips.remove(basketIdx);
            return;
        }

        // 選択された食べ物のツールチップを取得（再帰防止）
        isGettingFoodTooltip = true;
        List<Component> foodTooltips;
        try {
            foodTooltips = food.getTooltipLines(player, TooltipFlag.Default.NORMAL);
        } finally {
            isGettingFoodTooltip = false;
        }

        int foodIdx = findNBLine(foodTooltips, nutrientsLabel);
        if (foodIdx < 0) {
            tooltips.remove(basketIdx);
            return;
        }

        String foodNBText = foodTooltips.get(foodIdx).getString();
        float multiplier = FoodCalculator.CalculateMultiplier(food, player);
        if (multiplier < 1.0f) {
            foodNBText = applyScale(foodNBText, multiplier);
        }
        tooltips.set(basketIdx, Component.nullToEmpty(foodNBText));
    }

    /**
     * NB行のインデックスを返す。色コード(§)を除去してからラベル検索する。
     * 見つからない場合は -1。
     */
    private static int findNBLine(List<Component> tooltips, String nutrientsLabel) {
        for (int i = 0; i < tooltips.size(); i++) {
            String stripped = ChatFormatting.stripFormatting(tooltips.get(i).getString());
            if (stripped != null && stripped.startsWith(nutrientsLabel)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * NB行（色コード付き生テキスト）の品質値に倍率を適用する。
     * 形式: "§7Nutrients: §2Carbs§7 (5.0NU)§r" → 括弧内の数値をスケーリング
     */
    private static String applyScale(String text, float multiplier) {
        int openParen = text.lastIndexOf('(');
        int closeParen = text.lastIndexOf(')');
        if (openParen < 0 || closeParen <= openParen) return text;

        String inside = text.substring(openParen + 1, closeParen); // "5.0NU"
        String numStr = inside.replace("NU", "");

        try {
            float value = Float.parseFloat(numStr);
            float scaled = multiplier <= 0f ? 0f : Math.round(value * multiplier * 10f) / 10f;
            return text.substring(0, openParen + 1) + scaled + "NU" + text.substring(closeParen);
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
