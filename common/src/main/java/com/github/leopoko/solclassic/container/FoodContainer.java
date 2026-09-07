package com.github.leopoko.solclassic.container;

import com.github.leopoko.solclassic.utils.BasketBlacklist;
import net.minecraft.core.component.DataComponents;
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
     * ※ 1.21.1 では FOOD データコンポーネントの有無で判定します。
     */
    private boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }

    /**
     * バスケットへ新規に収納できるアイテムかどうかを判定します。
     * 食料であること、WickerBasket自体でないこと、
     * 設定 basketBlacklist に含まれないことを確認します。
     */
    private boolean canAccept(ItemStack stack) {
        if (!isFood(stack)) return false;
        // WickerBasket自体をバスケット内に入れることを防止
        // （1.21.1ではダミーのFOODコンポーネントを持つため isFood() では弾けない）
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId.toString().equals("solclassic:wicker_basket")) return false;
        // 設定でバスケットへの収納を禁止されたアイテム
        if (BasketBlacklist.isBlacklisted(stack)) return false;
        return true;
    }

    /**
     * バニラのSlot.mayPlace()が呼ぶバリデーション。
     * FoodSlotと同等のチェックをコンテナレベルでも行い、二重の安全策とする。
     */
    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return canAccept(stack);
    }

    /**
     * addItem をオーバーライドして、収納できないアイテムは受け付けないようにします。
     */
    @Override
    public @NotNull ItemStack addItem(@NotNull ItemStack itemStack) {
        if (!canAccept(itemStack)) {
            // 収納できない場合は何もせず、入力されたスタックをそのまま返す
            return itemStack;
        }
        return super.addItem(itemStack);
    }

    /**
     * canAddItem をオーバーライドして、収納できないアイテムの場合は false を返します。
     */
    @Override
    public boolean canAddItem(@NotNull ItemStack itemStack) {
        if (!canAccept(itemStack)) {
            return false;
        }
        return super.canAddItem(itemStack);
    }

    /**
     * setItem をオーバーライドして、食料アイテム以外はセットしないようにします。
     * ※ NBTからの復元（fromTag）もこの経路を通るため、
     *   ここに canAccept() の判定を入れてはいけない（既存の中身が消える）。
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
