package com.github.leopoko.solclassic.item;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.container.FoodChestMenu;
import com.github.leopoko.solclassic.container.FoodContainer;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);

            FoodContainer chestInventory = createInventoryFromItemStack(stack, registries);
            int slotIndex = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
            // カスタムMenuTypeでメニューを開く（クライアント・サーバー両方でFoodSlot+FoodContainerを使用）
            MenuRegistry.openExtendedMenu(serverPlayer, new ExtendedMenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.basket");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                    return new FoodChestMenu(Solclassic.FOOD_CHEST_MENU_TYPE, containerId, playerInventory, chestInventory, 1, slotIndex) {
                        @Override
                        public void removed(@NotNull Player playerIn) {
                            super.removed(playerIn);
                            // UI終了時に在庫情報をアイテムに保存する
                            saveInventoryToItemStack(stack, chestInventory, registries);
                        }
                    };
                }

                @Override
                public void saveExtraData(FriendlyByteBuf buf) {
                    buf.writeInt(1); // rows
                    buf.writeInt(slotIndex); // lockedSlotIndex
                }
            });
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
