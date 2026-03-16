package com.github.leopoko.solclassic.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 食料専用のチェストメニュー。
 *
 * ChestMenuを継承してslots.set()でスロットを差し替える方式では、
 * AbstractContainerMenuの内部リスト（lastSlots, remoteSlots）との不整合や、
 * Mohist等のBukkitブリッジ環境でスロットインデックスの問題が発生していた。
 *
 * この実装ではAbstractContainerMenuを直接継承し、addSlot()で最初から
 * 正しいスロットタイプ（FoodSlot, LockedSlot）を登録することで、
 * スロット差し替えに起因する不具合を根本的に解消する。
 */
public class FoodChestMenu extends AbstractContainerMenu {

    private final Container container;
    private final int rows;

    public FoodChestMenu(MenuType<?> menuType, int id, Inventory playerInventory, Container container, int rows, int lockedSlotIndex) {
        super(menuType, id);
        checkContainerSize(container, rows * 9);
        this.container = container;
        this.rows = rows;
        container.startOpen(playerInventory.player);

        int yOffset = (rows - 4) * 18;

        // コンテナスロット（食料専用スロットとして追加）
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new FoodSlot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // プレイヤーインベントリ（メイン3行）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + yOffset));
            }
        }

        // プレイヤーホットバー（バスケットを持っているスロットはロック）
        for (int col = 0; col < 9; col++) {
            if (col == lockedSlotIndex) {
                this.addSlot(new LockedSlot(playerInventory, col, 8 + col * 18, 161 + yOffset));
            } else {
                this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + yOffset));
            }
        }
    }

    public Container getContainer() {
        return this.container;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            if (index < this.rows * 9) {
                // コンテナ → プレイヤーインベントリ
                if (!this.moveItemStackTo(slotStack, this.rows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // プレイヤーインベントリ → コンテナ
                if (!this.moveItemStackTo(slotStack, 0, this.rows * 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
