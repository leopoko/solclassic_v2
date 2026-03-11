package com.github.leopoko.solclassic.item;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.container.FoodChestMenu;
import com.github.leopoko.solclassic.container.FoodContainer;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WickerBasketItem extends Item {

    private static final String INVENTORY_TAG = "basket_inventory";
    public static final int SLOT_COUNT = 9;

    /**
     * ダミーのFoodProperties。AppleSkinのFoodValuesEvent発火に必要。
     * nutritionとsaturationは0に設定し、NB/Dietが誤って栄養素を加算しないようにする。
     */
    private static final FoodProperties DUMMY_FOOD_PROPERTIES =
            new FoodProperties.Builder().nutrition(0).saturationModifier(0f).build();

    public WickerBasketItem(Properties properties) {
        super(properties);
    }

    /**
     * 食べるアニメーションの継続時間を返す。
     */
    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 32; // バニラの通常食べ物と同じ
    }

    /**
     * 食べるアニメーションの種類を返す。
     * FoodPropertiesがなくても食べるアニメーションを再生するためにオーバーライド。
     */
    @Override
    public @NotNull net.minecraft.world.item.UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return net.minecraft.world.item.UseAnim.EAT;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (entity instanceof Player player) {
            HolderLookup.Provider registries = level.registryAccess();

            // 食事履歴更新前に食べ物を確定する
            ItemStack food = getMostNutritiousFood(stack, player);

            if (!food.isEmpty()) {
                // 食べ物のコピーを作成し、食べ物自身のfinishUsingItem()に処理を委譲する。
                ItemStack foodCopy = food.copy();
                foodCopy.setCount(1);
                ItemStack result = foodCopy.finishUsingItem(level, entity);

                if (!level.isClientSide) {
                    // バスケットから食べ物を消費
                    shrinkItemFromInventory(stack, food, registries);

                    // 容器アイテムの返却（戻り値がボウルやガラス瓶の場合）
                    if (!result.isEmpty() && !result.is(food.getItem())) {
                        if (!player.getInventory().add(result)) {
                            player.drop(result, false);
                        }
                    }
                }
            }

            return stack;
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (!SolclassicConfigData.enableItemDescription) return;
        tooltip.add(Component.translatable("tooltip.wicker_basket.description1"));
        tooltip.add(Component.translatable("tooltip.wicker_basket.description2"));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        HolderLookup.Provider registries = level.registryAccess();

        if (player.isCrouching()) {
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
                        Component.translatable("container.wicker_basket")
                ));
            }
        }
        else
        {
            ItemStack stack = player.getItemInHand(hand);
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            FoodContainer container = createInventoryFromItemStack(stack, registries);
            ListTag listTag = new ListTag();
            if (tag.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
                listTag = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
                container.fromTag(listTag, registries);
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

    private static void shrinkItemFromInventory(ItemStack wickerbasket, ItemStack stack, HolderLookup.Provider registries) {
        FoodContainer container = createInventoryFromItemStack(wickerbasket, registries);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            if (itemStack.getItem() == stack.getItem()) {
                itemStack.shrink(1);
                if (itemStack.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
                break;
            }
        }
        saveInventoryToItemStack(wickerbasket, container, registries);
    }

    public static void shrinkMostNutritiousItemFromInventory(ItemStack wickerbasket, Player player) {
        HolderLookup.Provider registries = player.registryAccess();
        ItemStack mostNutritiousItemStack = getMostNutritiousFood(wickerbasket, player);
        if (!mostNutritiousItemStack.isEmpty()) {
            shrinkItemFromInventory(wickerbasket, mostNutritiousItemStack, registries);
        }
    }

    public static ItemStack getMostNutritiousFood(ItemStack stack, Player player) {
        int mostNutrition = 0;
        HolderLookup.Provider registries = player.registryAccess();

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        FoodContainer container = createInventoryFromItemStack(stack, registries);
        ListTag listTag = new ListTag();

        ItemStack mostNutritiousItemStack = ItemStack.EMPTY;

        if (!SolclassicConfigData.enableWickerBasket){
            return mostNutritiousItemStack;
        }

        if (tag.contains(INVENTORY_TAG, Tag.TAG_LIST)) {
            listTag = tag.getList(INVENTORY_TAG, Tag.TAG_COMPOUND);
            container.fromTag(listTag, registries);
        }

        if (listTag.isEmpty()) {
            return mostNutritiousItemStack;
        }

        for(int i = 0; i < listTag.size(); ++i) {
            ItemStack itemStack = ItemStack.parseOptional(registries, listTag.getCompound(i));
            if (!itemStack.isEmpty()) {
                FoodProperties foodProperties = itemStack.get(DataComponents.FOOD);
                if (foodProperties == null) {
                    continue;
                }
                // Quality Food等のMODによる品質修正を反映した栄養値を取得
                int nutrition = FoodHistoryHolder.INSTANCE.getEffectiveNutrition(itemStack, player);
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
