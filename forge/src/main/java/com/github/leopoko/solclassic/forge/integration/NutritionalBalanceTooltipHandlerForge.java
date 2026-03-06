package com.github.leopoko.solclassic.forge.integration;

import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Nutritional Balance MODツールチップ連携（Forge版・クライアント専用）。
 * NB MODが追加したツールチップの食品品質値にSoL Classicの減衰倍率を適用する。
 *
 * NB MODのツールチップ形式: "Nutrients: Protein,Carbs (5.0)"
 * → リテラル文字列（Component.nullToEmpty）で追加されるため、
 *    文字列パターンマッチで品質値を特定しスケーリングする。
 *
 * EventPriority.LOW で登録することで、NB MODのツールチップ追加（NORMAL優先度）の後に実行される。
 */
public class NutritionalBalanceTooltipHandlerForge {

    /**
     * Forgeイベントバスにクライアント側ツールチップハンドラを登録する。
     * Nutritional Balance MODがロードされている場合、FMLClientSetupEvent 内でのみ呼び出すこと。
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(NutritionalBalanceTooltipHandlerForge.class);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        ItemStack stack = event.getItemStack();

        // WickerBasketの場合は選択された食べ物の倍率を使用
        float multiplier;
        if (stack.getItem() instanceof WickerBasketItem) {
            ItemStack food = WickerBasketItem.getMostNutritiousFood(stack, player);
            if (food.isEmpty()) return;
            multiplier = FoodCalculator.CalculateMultiplier(food, player);
        } else {
            if (!stack.getItem().isEdible()) return;
            multiplier = FoodCalculator.CalculateMultiplier(stack, player);
        }

        modifyNBTooltips(event.getToolTip(), multiplier);
    }

    /**
     * NB MODのツールチップエントリにSoL Classicの減衰倍率を適用する。
     * NB MODのツールチップ形式: "[Nutrients]: [NutrientNames] ([quality])"
     * → 品質値（括弧内の数値）に倍率を適用する。
     *
     * @param tooltips ツールチップのComponentリスト
     * @param multiplier SoL Classicの減衰倍率（0.0〜1.0、1.0は減衰なし）
     */
    private static void modifyNBTooltips(List<Component> tooltips, float multiplier) {
        // 減衰なし（倍率100%）の場合は変更不要
        if (multiplier >= 1.0f) return;

        // NB MODの "Nutrients" ラベルを翻訳キーから取得
        String nutrientsLabel = I18n.get("nutritionalbalance.nutrients");

        for (int i = 0; i < tooltips.size(); i++) {
            Component component = tooltips.get(i);
            String text = component.getString();

            // NBのツールチップ行を特定: "[Nutrients]: ..." で始まる行
            if (!text.startsWith(nutrientsLabel)) continue;

            // 品質値の括弧を検索: "Nutrients: Protein,Carbs (5.0)"
            int openParen = text.lastIndexOf('(');
            int closeParen = text.lastIndexOf(')');

            if (openParen > 0 && closeParen == text.length() - 1 && closeParen > openParen) {
                String valueStr = text.substring(openParen + 1, closeParen).trim();
                try {
                    // ロケール差異（カンマ/ピリオド）を吸収してパース
                    float value = Float.parseFloat(valueStr.replace(',', '.'));
                    float scaledValue;
                    if (multiplier <= 0.0f) {
                        scaledValue = 0.0f;
                    } else {
                        // NB MODと同じ丸め方式（小数点1桁）
                        scaledValue = Math.round(value * multiplier * 10.0f) / 10.0f;
                    }
                    String newText = text.substring(0, openParen + 1) + scaledValue + text.substring(closeParen);
                    tooltips.set(i, Component.nullToEmpty(newText));
                } catch (NumberFormatException e) {
                    // パースできない場合はスキップ
                }
            }
            break; // NBのツールチップ行は1つだけ
        }
    }
}
