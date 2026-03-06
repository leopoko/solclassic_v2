package com.github.leopoko.solclassic.fabric.integration;

import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.ClientTooltipHandler;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietResult;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Diet MODツールチップ連携（Fabric版・クライアント専用）。
 * Diet MODが追加したツールチップの栄養グループ値にSoL Classicの減衰倍率を適用する。
 * WickerBasketの場合はDietがツールチップを追加しないため、中の食べ物に基づいて自前で生成する。
 *
 * Fabric APIのイベントフェーズ機能を使い、DEFAULT_PHASEの後に実行される
 * カスタムフェーズで登録することで、Diet MODのツールチップ追加後に確実に実行される。
 */
public class DietTooltipHandlerFabric {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.#");

    /**
     * Fabric APIのItemTooltipCallbackにクライアント側ツールチップハンドラを登録する。
     * Diet MODがロードされている場合、ClientModInitializer 内でのみ呼び出すこと。
     */
    public static void register() {
        // Diet MODのデフォルトフェーズの後に実行されるカスタムフェーズを定義
        ResourceLocation afterDiet = new ResourceLocation("solclassic", "after_diet");
        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, afterDiet);

        ItemTooltipCallback.EVENT.register(afterDiet, (stack, context, lines) -> {
            if (!stack.getItem().isEdible()) return;

            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            if (stack.getItem() instanceof WickerBasketItem) {
                // WickerBasket: DietがWickerBasket自体のFoodPropertiesで追加したエントリを削除し、
                // 中の最も栄養価の高い食べ物の情報で再生成する
                ClientTooltipHandler.removeDietTooltips(lines);
                ItemStack actualFood = WickerBasketItem.getMostNutritiousFood(stack, player);
                if (actualFood.isEmpty()) return;
                float multiplier = FoodCalculator.CalculateMultiplier(actualFood, player);
                addDietTooltipsForFood(lines, player, actualFood, multiplier);
            } else {
                // 通常の食べ物: Dietが追加済みのツールチップエントリを修正
                float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
                ClientTooltipHandler.modifyDietTooltips(lines, multiplier);
            }
        });
    }

    /**
     * WickerBasket用: DietApiから栄養グループ情報を取得し、減衰倍率を適用したツールチップを追加する。
     */
    private static void addDietTooltipsForFood(List<Component> tooltips, Player player, ItemStack food, float multiplier) {
        if (multiplier <= 0.0f) return;

        IDietResult result = DietApi.getInstance().get(player, food);
        Map<IDietGroup, Float> groups = result.get();
        if (groups.isEmpty()) return;

        List<Component> beneficial = new ArrayList<>();
        List<Component> harmful = new ArrayList<>();

        for (Map.Entry<IDietGroup, Float> entry : groups.entrySet()) {
            float value = entry.getValue() * multiplier;
            if (value > 0.0f) {
                Component groupName = Component.translatable(
                        "groups.diet." + entry.getKey().getName() + ".name");
                MutableComponent tooltip = Component.translatable(
                        "tooltip.diet.group",
                        DECIMAL_FORMAT.format(value * 100), groupName);

                if (entry.getKey().isBeneficial()) {
                    tooltip.withStyle(ChatFormatting.GREEN);
                    beneficial.add(tooltip);
                } else {
                    tooltip.withStyle(ChatFormatting.RED);
                    harmful.add(tooltip);
                }
            }
        }

        if (!beneficial.isEmpty() || !harmful.isEmpty()) {
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("tooltip.diet.eaten")
                    .withStyle(ChatFormatting.GRAY));
            tooltips.addAll(beneficial);
            tooltips.addAll(harmful);
        }
    }
}
