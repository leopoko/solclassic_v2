package com.github.leopoko.solclassic.fabric.integration;

import com.github.leopoko.solclassic.utils.ClientTooltipHandler;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Diet MODツールチップ連携（Fabric版・クライアント専用）。
 * Diet MODが追加したツールチップの栄養グループ値にSoL Classicの減衰倍率を適用する。
 *
 * Fabric APIのイベントフェーズ機能を使い、DEFAULT_PHASEの後に実行される
 * カスタムフェーズで登録することで、Diet MODのツールチップ追加後に確実に実行される。
 */
public class DietTooltipHandlerFabric {

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

            float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
            ClientTooltipHandler.modifyDietTooltips(lines, multiplier);
        });
    }
}
