package com.github.leopoko.solclassic.fabric.foodhistory;

import com.github.leopoko.solclassic.utils.FoodHistory;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public interface IFoodHistoryComponentFabric extends Component, AutoSyncedComponent {
    /**
     * 現在の食事履歴を返す
     */
    FoodHistory getHistory();

    /**
     * 渡された食事履歴で内部データを上書きする
     * @param newHistory 外部から渡された食事履歴
     */
    void setFood(FoodHistory newHistory);
}
