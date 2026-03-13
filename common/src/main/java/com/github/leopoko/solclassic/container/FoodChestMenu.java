package com.github.leopoko.solclassic.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

public class FoodChestMenu extends ChestMenu {

    public FoodChestMenu(MenuType<?> menuType, int id, Inventory playerInventory, Container container, int rows, int lockedSlotIndex) {
        super(menuType, id, playerInventory, container, rows);
        // すべてのスロットを食料専用スロットに変更
        for (int i = 0; i < container.getContainerSize(); i++) {
            FoodSlot foodSlot = new FoodSlot(container, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18);
            foodSlot.index = i;
            this.slots.set(i, foodSlot);
        }

        // プレイヤーのインベントリスロットに対する制限（バスケットを持っているスロットをロック）
        if (lockedSlotIndex >= 0 && lockedSlotIndex < playerInventory.items.size()) {
            int menuSlotIndex = rows * 9 + 27 + lockedSlotIndex;
            LockedSlot lockedSlot = new LockedSlot(playerInventory, lockedSlotIndex, 8 + (lockedSlotIndex % 9) * 18, 142 + (lockedSlotIndex / 9) * 18);
            lockedSlot.index = menuSlotIndex;
            this.slots.set(menuSlotIndex, lockedSlot);
        }
    }
}