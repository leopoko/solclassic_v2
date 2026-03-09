package com.github.leopoko.solclassic.utils;

import com.github.leopoko.solclassic.client.FoodHistoryBookScreen;
import com.github.leopoko.solclassic.item.FoodHistoryBookItem;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.text.DecimalFormat;
import java.util.List;

public class ClientTooltipHandler {
    public static void init() {
        // クライアント環境でのみFoodHistoryBookItemの画面オープン処理を登録
        FoodHistoryBookItem.screenOpener = FoodHistoryBookScreen::open;

        ClientTooltipEvent.ITEM.register((ItemStack stack, List<Component> tooltips, Item.TooltipContext context, TooltipFlag flag) -> {
            if (stack.getItem() instanceof WickerBasketItem) {
                // WickerBasket: 選択された食べ物の情報を表示
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    ItemStack food = WickerBasketItem.getMostNutritiousFood(stack, player);
                    if (!food.isEmpty()) {
                        FoodProperties foodProps = food.get(DataComponents.FOOD);
                        if (foodProps != null) {
                            float multiplier = FoodCalculator.CalculateMultiplier(food, player);
                            int nutrition = FoodCalculator.CalculateNutrition(foodProps.nutrition(), multiplier);
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
            } else if (stack.has(DataComponents.FOOD)) {
                // 通常の食べ物: 減衰率のみ表示
                FoodProperties foodProps = stack.get(DataComponents.FOOD);
                if (foodProps != null) {

                    if (Minecraft.getInstance().player != null) {
                        Player player = Minecraft.getInstance().player;

                        // WickerBasketの場合、中の最も栄養価の高い食べ物で計算
                        ItemStack targetFood = stack;
                        if (stack.getItem() instanceof WickerBasketItem) {
                            targetFood = WickerBasketItem.getMostNutritiousFood(stack, player);
                            if (targetFood.isEmpty()) return;
                        }

                        float multiplier = FoodCalculator.CalculateMultiplier(targetFood, player);
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
     * Diet MODが追加したツールチップエントリをすべて削除する。
     */
    public static void removeDietTooltips(List<Component> tooltips) {
        for (int i = tooltips.size() - 1; i >= 0; i--) {
            Component component = tooltips.get(i);
            if (!(component.getContents() instanceof TranslatableContents tc)) continue;
            String key = tc.getKey();

            if ("tooltip.diet.group".equals(key) || "tooltip.diet.group_".equals(key)) {
                tooltips.remove(i);
            } else if ("tooltip.diet.eaten".equals(key)) {
                tooltips.remove(i);
                if (i > 0 && tooltips.get(i - 1).getString().isEmpty()) {
                    tooltips.remove(i - 1);
                    i--;
                }
            }
        }
    }

    /**
     * Diet MODのツールチップエントリにSoL Classicの減衰倍率を適用する。
     */
    public static void modifyDietTooltips(List<Component> tooltips, float multiplier) {
        if (multiplier >= 1.0f) return;

        boolean removedEntries = false;

        for (int i = tooltips.size() - 1; i >= 0; i--) {
            Component component = tooltips.get(i);
            if (!(component.getContents() instanceof TranslatableContents tc)) continue;
            String key = tc.getKey();

            if ("tooltip.diet.group".equals(key)) {
                if (multiplier <= 0.0f) {
                    tooltips.remove(i);
                    removedEntries = true;
                } else {
                    Object[] args = tc.getArgs();
                    if (args.length >= 2 && args[0] instanceof String percentStr) {
                        try {
                            float originalPercent = Float.parseFloat(percentStr.replace(',', '.'));
                            float scaledPercent = originalPercent * multiplier;
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
                tooltips.remove(i);
                removedEntries = true;
            }
        }

        if (removedEntries) {
            for (int i = tooltips.size() - 1; i >= 0; i--) {
                Component component = tooltips.get(i);
                if (component.getContents() instanceof TranslatableContents tc
                        && "tooltip.diet.eaten".equals(tc.getKey())) {
                    tooltips.remove(i);
                    if (i > 0 && tooltips.get(i - 1).getString().isEmpty()) {
                        tooltips.remove(i - 1);
                    }
                    break;
                }
            }
        }
    }
}
