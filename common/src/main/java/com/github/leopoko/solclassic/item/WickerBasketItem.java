package com.github.leopoko.solclassic.item;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.container.FoodChestMenu;
import com.github.leopoko.solclassic.container.FoodContainer;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WickerBasketItem extends Item {

    private static final String INVENTORY_TAG = "basket_inventory";
    public static final int SLOT_COUNT = 9;

    public WickerBasketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wicker_basket.description1"));
        tooltip.add(Component.translatable("tooltip.wicker_basket.description2"));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        if (player.isCrouching()) {
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
                        Component.translatable("container.wicker_basket")
                ));
            }
        }
        else
        {
            ItemStack stack = player.getItemInHand(hand);
            CompoundTag tag = stack.getTag();
            FoodContainer container = createInventoryFromItemStack(stack);
            ListTag listTag = new ListTag();
            if (tag != null && tag.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
                listTag = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
                container.fromTag(listTag);
            }

            if (listTag.isEmpty() || !SolclassicConfigData.enableWickerBasket) {
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
            }
            else {
                player.startUsingItem(hand);
            }
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

    private static void shrinkItemFromInventory(ItemStack wickerbasket, ItemStack stack) {
        CompoundTag tag = wickerbasket.getTag();
        FoodContainer container = createInventoryFromItemStack(wickerbasket);
        ListTag listTag = new ListTag();
        if (tag != null && tag.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
            listTag = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
            container.fromTag(listTag);
        }
        boolean found = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            String itemId_a = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
            String itemId_b = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (itemId_a.equals(itemId_b)) {
                //container.removeItem(i, 1);
                itemStack.shrink(1);
                if (itemStack.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
                found = true;
                break;
            }
        }
        saveInventoryToItemStack(wickerbasket, container);
    }

    public static void shrinkMostNutritiousItemFromInventory(ItemStack wickerbasket, Player player) {
        CompoundTag tag = wickerbasket.getTag();
        FoodContainer container = createInventoryFromItemStack(wickerbasket);
        ListTag listTag = new ListTag();
        if (tag != null && tag.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
            listTag = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
            container.fromTag(listTag);
        }
        ItemStack mostNutritiousItemStack = getMostNutritiousFood(wickerbasket, player);
        if (!mostNutritiousItemStack.isEmpty()) {
            shrinkItemFromInventory(wickerbasket, mostNutritiousItemStack);
        }
    }

    public static ItemStack getMostNutritiousFood(ItemStack stack, Player player) {
        int mostNutrition = 0;

        CompoundTag tag = stack.getTag();
        FoodContainer container = createInventoryFromItemStack(stack);
        ListTag listTag = new ListTag();

        ItemStack mostNutritiousItemStack = ItemStack.EMPTY;

        if (!SolclassicConfigData.enableWickerBasket){
            return mostNutritiousItemStack;
        }

        if (tag != null && tag.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
            listTag = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
            container.fromTag(listTag);
        }

        if (listTag.isEmpty()) {
            return mostNutritiousItemStack;
        }

        for(int i = 0; i < listTag.size(); ++i) {
            ItemStack itemStack = ItemStack.of(listTag.getCompound(i));
            if (!itemStack.isEmpty()) {
                if (itemStack.getItem().getFoodProperties() == null) {
                    continue;
                }
                int nutrition = itemStack.getItem().getFoodProperties().getNutrition();
                float Multiplier = FoodCalculator.CalculateMultiplier(itemStack, player);
                nutrition = FoodCalculator.CalculateNutrition(nutrition, Multiplier);
                if (mostNutrition < nutrition) {
                    mostNutrition = nutrition;
                    mostNutritiousItemStack = itemStack;
                }
            }
        }
        return mostNutritiousItemStack;
    }
}
