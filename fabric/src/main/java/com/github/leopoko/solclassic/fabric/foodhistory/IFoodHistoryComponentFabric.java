package com.github.leopoko.solclassic.fabric.foodhistory;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public interface IFoodHistoryComponentFabric extends Component, AutoSyncedComponent {
    /**
     * 現在の食事履歴（ItemStack の LinkedList）を返す
     */
    LinkedList<ItemStack> getHistory();

    /**
     * 渡された食事履歴で内部データを上書きする
     * @param newHistory 外部から渡された食事履歴
     */
    void setFood(LinkedList<ItemStack> newHistory);
}