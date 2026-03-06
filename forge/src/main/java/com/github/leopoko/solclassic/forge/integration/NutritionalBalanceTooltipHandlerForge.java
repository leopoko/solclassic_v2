package com.github.leopoko.solclassic.forge.integration;

import com.dannyandson.nutritionalbalance.nutrients.Nutrient;
import com.dannyandson.nutritionalbalance.nutrients.WorldNutrients;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.StringJoiner;

/**
 * Nutritional Balance MODツールチップ連携（Forge版・クライアント専用）。
 * NB APIを直接呼び出して栄養素・品質値を取得し、減衰倍率を適用した上で
 * NBのツールチップ行を差し替える。文字列パースは行わない。
 *
 * - WickerBasket: 選択された食べ物の栄養素・品質値（減衰適用済み）を表示
 * - 通常食べ物: 品質値に減衰倍率を適用
 */
public class NutritionalBalanceTooltipHandlerForge {

    public static void register() {
        MinecraftForge.EVENT_BUS.register(NutritionalBalanceTooltipHandlerForge.class);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        ItemStack stack = event.getItemStack();

        // 対象アイテムと倍率を決定
        Item targetItem;
        float multiplier;

        if (stack.getItem() instanceof WickerBasketItem) {
            ItemStack food = WickerBasketItem.getMostNutritiousFood(stack, player);
            if (food.isEmpty()) {
                removeNBLine(event.getToolTip());
                return;
            }
            targetItem = food.getItem();
            multiplier = FoodCalculator.CalculateMultiplier(food, player);
        } else if (stack.getItem().isEdible()) {
            multiplier = FoodCalculator.CalculateMultiplier(stack, player);
            if (multiplier >= 1.0f) return;
            targetItem = stack.getItem();
        } else {
            return;
        }

        replaceNBLine(event.getToolTip(), targetItem, player.level(), multiplier);
    }

    /**
     * NB APIから栄養素・品質値を取得し、減衰適用済みのツールチップ行に差し替える。
     */
    private static void replaceNBLine(List<Component> tooltips, Item item, Level level, float multiplier) {
        int idx = findNBLineIndex(tooltips);
        if (idx < 0) return;

        try {
            List<Nutrient> nutrients = WorldNutrients.getNutrients(item, level);
            if (nutrients.isEmpty()) return;

            StringJoiner names = new StringJoiner(",");
            for (Nutrient n : nutrients) {
                names.add(n.getLocalizedName());
            }

            FoodProperties foodProps = item.getFoodProperties();
            String qualityStr = "";
            if (foodProps != null) {
                float quality = WorldNutrients.getEffectiveFoodQuality(foodProps, nutrients.size());
                quality = Math.round(quality * 10f) / 10f;
                if (multiplier < 1.0f) {
                    quality = multiplier <= 0f ? 0f : Math.round(quality * multiplier * 10f) / 10f;
                }
                qualityStr = " (" + quality + "NU)";
            }

            String label = I18n.get("nutritionalbalance.nutrients");
            // NBと同じ書式: §7label: §2nutrients§7 (qualityNU)§r
            tooltips.set(idx, Component.nullToEmpty("\u00a77" + label + ": \u00a72" + names + "\u00a77" + qualityStr + "\u00a7r"));
        } catch (Exception e) {
            // NB API呼び出しエラーは無視
        }
    }

    /** NBが追加したツールチップ行のインデックスを返す。見つからない場合は -1。 */
    private static int findNBLineIndex(List<Component> tooltips) {
        String label = I18n.get("nutritionalbalance.nutrients");
        for (int i = 0; i < tooltips.size(); i++) {
            if (tooltips.get(i).getString().contains(label)) {
                return i;
            }
        }
        return -1;
    }

    /** NBのツールチップ行を削除する */
    private static void removeNBLine(List<Component> tooltips) {
        int idx = findNBLineIndex(tooltips);
        if (idx >= 0) tooltips.remove(idx);
    }
}
