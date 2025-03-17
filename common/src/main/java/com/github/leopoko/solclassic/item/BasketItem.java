package com.github.leopoko.solclassic.item;

import com.github.leopoko.solclassic.container.FoodChestMenu;
import com.github.leopoko.solclassic.container.FoodContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BasketItem extends Item {

    private static final String INVENTORY_TAG = "lunchbasket_inventory";
    public static final int SLOT_COUNT = 9;

    public BasketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer) {
            ItemStack stack = player.getItemInHand(hand);

            FoodContainer chestInventory = createInventoryFromItemStack(stack);
            int slotIndex = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
            // バニラのチェストUIを表示
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new FoodChestMenu(MenuType.GENERIC_9x1, containerId, playerInventory, chestInventory, 1, slotIndex) {
                        @Override
                        public void removed(@NotNull Player playerIn) {
                            super.removed(playerIn);
                            // UI終了時に在庫情報をアイテムに保存する
                            saveInventoryToItemStack(stack, chestInventory);
                        }
                    },
                    Component.translatable("container.basket")
            ));
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private static FoodContainer createInventoryFromItemStack(ItemStack stack) {
        FoodContainer container = new FoodContainer(SLOT_COUNT);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
            container.fromTag(listTag);
        }
        return container;
    }

    private static void saveInventoryToItemStack(ItemStack stack, SimpleContainer container) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(INVENTORY_TAG, container.createTag());
        stack.setTag(tag);
    }
}
