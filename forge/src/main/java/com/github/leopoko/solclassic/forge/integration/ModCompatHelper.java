package com.github.leopoko.solclassic.forge.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * 他MODとの互換性を保つためのヘルパークラス。
 * WickerBasketから食べ物を消費した際に、LivingEntityUseItemEvent.Finishを
 * 実際の食べ物アイテムで発火させることで、Nutritional Balance等の
 * 食事イベントを監視するMODに正しく通知する。
 */
public class ModCompatHelper {

    /**
     * WickerBasketから食べ物が消費された際に、実際の食べ物アイテムで
     * LivingEntityUseItemEvent.Finishイベントを発火する。
     * これにより、Nutritional Balance等のMODが食べた食べ物を正しく認識できる。
     *
     * @param player    食べたプレイヤー
     * @param foodItem  実際に消費された食べ物のItemStack
     */
    public static void notifyFoodConsumedFromBasket(ServerPlayer player, ItemStack foodItem) {
        if (foodItem.isEmpty() || foodItem.getItem().getFoodProperties() == null) {
            return;
        }
        // 消費後のアイテムスタックをシミュレート
        ItemStack resultStack = foodItem.copy();
        resultStack.shrink(1);
        LivingEntityUseItemEvent.Finish event = new LivingEntityUseItemEvent.Finish(
                player, foodItem, 0, resultStack
        );
        MinecraftForge.EVENT_BUS.post(event);
    }
}
