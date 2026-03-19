package com.github.leopoko.solclassic.container;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FoodContainer extends SimpleContainer implements Container, StackedContentsCompatible {

    public FoodContainer(int size) {
        super(size);
    }

    public FoodContainer(ItemStack... itemStacks) {
        super(itemStacks);
    }

    /**
     * 食料アイテムかどうかを判定します。
     * ※ 食料アイテムの場合、Item#isEdible() が true を返します。
     */
    private boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().isEdible();
    }

    /**
     * バニラのSlot.mayPlace()が呼ぶバリデーション。
     * FoodSlotと同等のチェックをコンテナレベルでも行い、二重の安全策とする。
     */
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (!isFood(stack)) return false;
        // WickerBasket自体をバスケット内に入れることを防止
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId.toString().equals("solclassic:wicker_basket")) return false;
        return true;
    }

    /**
     * addItem をオーバーライドして、食料アイテム以外は受け付けないようにします。
     */
    @Override
    public @NotNull ItemStack addItem(@NotNull ItemStack itemStack) {
        if (!isFood(itemStack)) {
            // 食料アイテムでない場合は何もせず、入力されたスタックをそのまま返す
            return itemStack;
        }
        return super.addItem(itemStack);
    }

    /**
     * canAddItem をオーバーライドして、食料アイテムでない場合は false を返します。
     */
    @Override
    public boolean canAddItem(@NotNull ItemStack itemStack) {
        if (!isFood(itemStack)) {
            return false;
        }
        return super.canAddItem(itemStack);
    }

    /**
     * setItem をオーバーライドして、食料アイテム以外はセットしないようにします。
     */
    @Override
    public void setItem(int index, ItemStack itemStack) {
        if (itemStack.isEmpty() || isFood(itemStack)) {
            super.setItem(index, itemStack);
        }
        // 食料以外の場合は何もしない（または必要に応じて空にするなどの処理を追加してください）
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int index) {
        ItemStack removed = super.removeItemNoUpdate(index);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }
}
