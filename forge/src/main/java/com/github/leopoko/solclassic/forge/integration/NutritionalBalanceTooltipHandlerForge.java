package com.github.leopoko.solclassic.forge.integration;

import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodCalculator;
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
 * NBツールチップ形式: "Nutrients: Carbs(5.0NU)" （Component.nullToEmpty リテラル文字列）
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
            // WickerBasket: 選択された食べ物のNBツールチップに差し替え
            replaceWithFoodNBLine(tooltips, nutrientsLabel, stack, player);
            return;
        }

        if (!stack.getItem().isEdible()) return;

        float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
        if (multiplier >= 1.0f) return;

        scaleNBQuality(tooltips, nutrientsLabel, multiplier);
    }

    /**
     * WickerBasketのNBツールチップを、選択された食べ物のNBツールチップに差し替える。
     * 減衰倍率も適用する。
     */
    private static void replaceWithFoodNBLine(List<Component> tooltips, String nutrientsLabel,
                                               ItemStack basketStack, Player player) {
        ItemStack food = WickerBasketItem.getMostNutritiousFood(basketStack, player);
        if (food.isEmpty()) {
            // 食べ物がない場合はNB行を削除
            removeNBLine(tooltips, nutrientsLabel);
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

        // 食べ物のNB行を検索
        String foodNBLine = null;
        for (Component c : foodTooltips) {
            String text = c.getString();
            if (text.startsWith(nutrientsLabel)) {
                foodNBLine = text;
                break;
            }
        }

        if (foodNBLine == null) {
            // 食べ物にNB行がない場合はバスケットのNB行を削除
            removeNBLine(tooltips, nutrientsLabel);
            return;
        }

        // バスケットのNB行を食べ物のNB行に差し替え（倍率適用）
        float multiplier = FoodCalculator.CalculateMultiplier(food, player);
        for (int i = 0; i < tooltips.size(); i++) {
            if (tooltips.get(i).getString().startsWith(nutrientsLabel)) {
                if (multiplier < 1.0f) {
                    tooltips.set(i, Component.nullToEmpty(applyScale(foodNBLine, multiplier)));
                } else {
                    tooltips.set(i, Component.nullToEmpty(foodNBLine));
                }
                return;
            }
        }
    }

    /** NBのツールチップ行を削除する */
    private static void removeNBLine(List<Component> tooltips, String nutrientsLabel) {
        for (int i = tooltips.size() - 1; i >= 0; i--) {
            if (tooltips.get(i).getString().startsWith(nutrientsLabel)) {
                tooltips.remove(i);
                break;
            }
        }
    }

    /**
     * NB行の品質値に倍率を適用した文字列を返す。
     * 形式例: "Nutrients: Protein,Carbs(5.0NU)" → 括弧内の数値部分をスケーリング
     */
    private static String applyScale(String text, float multiplier) {
        int openParen = text.lastIndexOf('(');
        int closeParen = text.lastIndexOf(')');
        if (openParen < 0 || closeParen <= openParen) return text;

        String inside = text.substring(openParen + 1, closeParen).trim();

        // "5.0NU" → 数値部分とサフィックスを分離
        String suffix = "";
        String numStr = inside;
        int numEnd = numStr.length();
        while (numEnd > 0 && !Character.isDigit(numStr.charAt(numEnd - 1)) && numStr.charAt(numEnd - 1) != '.') {
            numEnd--;
        }
        if (numEnd < numStr.length()) {
            suffix = numStr.substring(numEnd);
            numStr = numStr.substring(0, numEnd);
        }

        try {
            float value = Float.parseFloat(numStr.replace(',', '.'));
            float scaled = (multiplier <= 0.0f) ? 0.0f
                    : Math.round(value * multiplier * 10.0f) / 10.0f;
            return text.substring(0, openParen + 1) + scaled + suffix + text.substring(closeParen);
        } catch (NumberFormatException e) {
            return text;
        }
    }

    /**
     * 通常食べ物のNBの品質値に減衰倍率を適用する。
     */
    private static void scaleNBQuality(List<Component> tooltips, String nutrientsLabel, float multiplier) {
        for (int i = 0; i < tooltips.size(); i++) {
            String text = tooltips.get(i).getString();
            if (!text.startsWith(nutrientsLabel)) continue;

            String scaled = applyScale(text, multiplier);
            if (!scaled.equals(text)) {
                tooltips.set(i, Component.nullToEmpty(scaled));
            }
            break;
        }
    }
}
