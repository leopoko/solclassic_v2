package com.github.leopoko.solclassic.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

public class FoodChestMenu extends ChestMenu {

    public FoodChestMenu(MenuType<?> menuType, int id, Inventory playerInventory, Container container, int rows, int lockedSlotIndex) {
        super(menuType, id, playerInventory, container, rows);
        // すべてのスロットを食料専用スロットに変更
        // slots.set() は addSlot() と異なり Slot.index を設定しないため、明示的に設定する
        for (int i = 0; i < container.getContainerSize(); i++) {
            FoodSlot newSlot = new FoodSlot(container, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18);
            newSlot.index = i;
            this.slots.set(i, newSlot);
        }

        // プレイヤーのインベントリスロットに対する制限
        if (lockedSlotIndex >= 0 && lockedSlotIndex < playerInventory.items.size()) {
            int menuSlotIndex = rows * 9 + 27 + lockedSlotIndex;
            LockedSlot lockedSlot = new LockedSlot(playerInventory, lockedSlotIndex, 8 + (lockedSlotIndex % 9) * 18, 142 + (lockedSlotIndex / 9) * 18);
            lockedSlot.index = menuSlotIndex;
            this.slots.set(menuSlotIndex, lockedSlot);
        }
    }
}