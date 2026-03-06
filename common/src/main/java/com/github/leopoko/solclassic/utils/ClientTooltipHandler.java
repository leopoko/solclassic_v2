package com.github.leopoko.solclassic.utils;

import com.github.leopoko.solclassic.item.WickerBasketItem;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.text.DecimalFormat;
import java.util.List;

public class ClientTooltipHandler {
    public static void init() {
        ClientTooltipEvent.ITEM.register((ItemStack stack, List<Component> tooltips, TooltipFlag flag) -> {
            if (stack.getItem() instanceof WickerBasketItem) {
                // WickerBasket: 選択された食べ物の情報を表示
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    ItemStack food = WickerBasketItem.getMostNutritiousFood(stack, player);
                    if (!food.isEmpty()) {
                        FoodProperties foodProps = food.getItem().getFoodProperties();
                        if (foodProps != null) {
                            float multiplier = FoodCalculator.CalculateMultiplier(food, player);
                            int nutrition = FoodCalculator.CalculateNutrition(foodProps.getNutrition(), multiplier);
                            // 選択された食べ物の名前と減衰後の栄養値を表示
                            tooltips.add(Component.translatable("tooltip.wicker_basket.food_info",
                                    food.getHoverName(), nutrition));
                            // 減衰率を表示
                            float reductionPercent = (1f - multiplier) * 100f;
                            String s = Integer.toString((int) reductionPercent);
                            if (!s.equals("0")) {
                                tooltips.add(Component.translatable("tooltip.food_reduction", s));
                            }
                        }
                    }
                }
            } else if (stack.getItem().isEdible()) {
                // 通常の食べ物: 減衰率のみ表示
                FoodProperties foodProps = stack.getItem().getFoodProperties();
                if (foodProps != null) {

                    if (Minecraft.getInstance().player != null) {
                        Player player = Minecraft.getInstance().player;
                        float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
                        multiplier = (1f - multiplier) * 100f;
                        String s = Integer.toString((int) multiplier);

                        if (!s.equals("0")) {
                            tooltips.add(Component.translatable("tooltip.food_reduction", s));
                        }
                    }
                }
            }
        });
    }

    /**
     * Diet MODのツールチップエントリにSoL Classicの減衰倍率を適用する。
     * Diet MODが追加したTranslatableComponent（キー "tooltip.diet.group"）のパーセンテージ値を
     * 倍率に応じてスケーリングする。
     *
     * @param tooltips ツールチップのComponentリスト（Dietのエントリが含まれる想定）
     * @param multiplier SoL Classicの減衰倍率（0.0〜1.0、1.0は減衰なし）
     */
    public static void modifyDietTooltips(List<Component> tooltips, float multiplier) {
        // 減衰なし（倍率100%）の場合は変更不要
        if (multiplier >= 1.0f) return;

        boolean removedEntries = false;

        for (int i = tooltips.size() - 1; i >= 0; i--) {
            Component component = tooltips.get(i);
            if (!(component.getContents() instanceof TranslatableContents tc)) continue;
            String key = tc.getKey();

            if ("tooltip.diet.group".equals(key)) {
                if (multiplier <= 0.0f) {
                    // 倍率0%: グループエントリを削除
                    tooltips.remove(i);
                    removedEntries = true;
                } else {
                    // 倍率を掛けてパーセンテージ値を再計算
                    Object[] args = tc.getArgs();
                    if (args.length >= 2 && args[0] instanceof String percentStr) {
                        try {
                            // ロケール差異（カンマ/ピリオド）を吸収してパース
                            float originalPercent = Float.parseFloat(percentStr.replace(',', '.'));
                            float scaledPercent = originalPercent * multiplier;
                            // Diet MODと同じフォーマットパターンで再作成
                            DecimalFormat df = new DecimalFormat("0.#");
                            tooltips.set(i, Component.translatable(
                                    key, df.format(scaledPercent), args[1]
                            ).withStyle(component.getStyle()));
                        } catch (NumberFormatException e) {
                            // パース失敗時はスキップ
                        }
                    }
                }
            } else if ("tooltip.diet.group_".equals(key) && multiplier <= 0.0f) {
                // special_foodタグ付きアイテムも倍率0%時は削除
                tooltips.remove(i);
                removedEntries = true;
            }
        }

        // 全グループエントリ削除時、"When eaten:" ヘッダーと空行も削除
        if (removedEntries) {
            for (int i = tooltips.size() - 1; i >= 0; i--) {
                Component component = tooltips.get(i);
                if (component.getContents() instanceof TranslatableContents tc
                        && "tooltip.diet.eaten".equals(tc.getKey())) {
                    tooltips.remove(i);
                    // ヘッダー直前の空行も削除
                    if (i > 0 && tooltips.get(i - 1).getString().isEmpty()) {
                        tooltips.remove(i - 1);
                    }
                    break;
                }
            }
        }
    }
}
