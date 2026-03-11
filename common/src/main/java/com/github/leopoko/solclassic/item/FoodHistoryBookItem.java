package com.github.leopoko.solclassic.item;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class FoodHistoryBookItem extends Item {

    /**
     * クライアント側で画面を開くためのコールバック。
     * サーバー側ではno-opのまま。ClientTooltipHandler.init()でクライアント環境のみ登録される。
     */
    public static Consumer<Player> screenOpener = player -> {};

    public FoodHistoryBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            screenOpener.accept(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (!SolclassicConfigData.enableItemDescription) return;
        tooltip.add(Component.translatable("tooltip.food_history_book.description"));
    }
}
