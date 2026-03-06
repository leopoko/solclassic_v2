package com.github.leopoko.solclassic.item;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.container.FoodChestMenu;
import com.github.leopoko.solclassic.container.FoodContainer;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
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

    /**
     * 食べるアニメーションの継続時間を返す。
     * WickerBasketはFoodPropertiesを持たないため（Diet/NB MODとの誤認防止）、
     * ここで明示的に食べる時間を指定する。
     */
    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
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
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof Player player) {
            // 食事履歴更新前に食べ物を確定する（eat()内のPlayerMixinが履歴を更新するため、
            // 更新後に取得すると減衰計算の変化で異なる結果になる可能性がある）
            ItemStack food = getMostNutritiousFood(stack, player);

            if (!food.isEmpty()) {
                // 食べ物のコピーを作成し、食べ物自身のfinishUsingItem()に処理を委譲する。
                // これにより以下がすべて食べ物固有の実装で処理される:
                //   - 栄養値の計算（PlayerMixin経由）
                //   - 食事履歴の記録（PlayerMixin経由）
                //   - ポーション効果の適用（LivingEntity.addEatEffect + SuspiciousStew等の固有処理）
                //   - ボウル/瓶などの容器アイテム返却（finishUsingItemの戻り値）
                ItemStack foodCopy = food.copy();
                foodCopy.setCount(1);
                ItemStack result = foodCopy.finishUsingItem(level, entity);

                if (!level.isClientSide) {
                    // バスケットから食べ物を消費
                    shrinkItemFromInventory(stack, food);

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
            if (itemStack.getItem() == stack.getItem()) {
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
