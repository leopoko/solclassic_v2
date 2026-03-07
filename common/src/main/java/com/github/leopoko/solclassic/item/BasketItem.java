package com.github.leopoko.solclassic.item;

import com.github.leopoko.solclassic.container.FoodChestMenu;
import com.github.leopoko.solclassic.container.FoodContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BasketItem extends Item {

    private static final String INVENTORY_TAG = "lunchbasket_inventory";
    public static final int SLOT_COUNT = 9;

    public BasketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, List<Component> tooltip, @NotNull TooltipFlag flag) {
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        HolderLookup.Provider registries = level.registryAccess();

        if (!level.isClientSide && player instanceof ServerPlayer) {
            ItemStack stack = player.getItemInHand(hand);

            FoodContainer chestInventory = createInventoryFromItemStack(stack, registries);
            int slotIndex = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
            // バニラのチェストUIを表示
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new FoodChestMenu(MenuType.GENERIC_9x1, containerId, playerInventory, chestInventory, 1, slotIndex) {
                        @Override
                        public void removed(@NotNull Player playerIn) {
                            super.removed(playerIn);
                            // UI終了時に在庫情報をアイテムに保存する
                            saveInventoryToItemStack(stack, chestInventory, registries);
                        }
                    },
                    Component.translatable("container.basket")
            ));
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private static FoodContainer createInventoryFromItemStack(ItemStack stack, HolderLookup.Provider registries) {
        FoodContainer container = new FoodContainer(SLOT_COUNT);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
            container.fromTag(listTag, registries);
        }
        return container;
    }

    private static void saveInventoryToItemStack(ItemStack stack, SimpleContainer container, HolderLookup.Provider registries) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(INVENTORY_TAG, container.createTag(registries));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
