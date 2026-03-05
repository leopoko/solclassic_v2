package com.github.leopoko.solclassic.forge.integration;

import com.github.leopoko.solclassic.utils.ClientTooltipHandler;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Diet MODツールチップ連携（Forge版・クライアント専用）。
 * Diet MODが追加したツールチップの栄養グループ値にSoL Classicの減衰倍率を適用する。
 *
 * EventPriority.LOW で登録することで、Diet MODのツールチップ追加（NORMAL優先度）の後に実行される。
 */
public class DietTooltipHandlerForge {

    /**
     * Forgeイベントバスにクライアント側ツールチップハンドラを登録する。
     * Diet MODがロードされている場合、FMLClientSetupEvent 内でのみ呼び出すこと。
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(DietTooltipHandlerForge.class);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        ItemStack stack = event.getItemStack();
        if (!stack.getItem().isEdible()) return;

        float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
        ClientTooltipHandler.modifyDietTooltips(event.getToolTip(), multiplier);
    }
}
