package com.github.leopoko.solclassic.neoforge.integration;

import com.dannyandson.nutritionalbalance.api.INutritionalBalancePlayer;
import com.dannyandson.nutritionalbalance.api.IPlayerNutrient;
import com.dannyandson.nutritionalbalance.nutrients.PlayerNutritionData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.FoodDecayTracker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nutritional Balance MOD連携（NeoForge版）。
 * SoL Classicの食事減衰倍率をNutritional Balanceの栄養素増加量にも反映する。
 *
 * 仕組み:
 * 1. HIGHEST優先度: NB処理前に各栄養素の値をスナップショットとして保存
 * 2. (NB がデフォルト優先度で栄養素を加算)
 * 3. LOWEST優先度: NB処理後にスナップショットと比較し、増加分に減衰倍率を適用
 *
 * Nutritional Balance MODがインストールされている場合のみロード・登録される。
 */
public class NutritionalBalanceIntegrationNeoForge {

    // NB処理前の栄養素値を一時保存するキャッシュ
    private static final Map<UUID, Map<String, Float>> preProcessValues = new ConcurrentHashMap<>();
    // HIGHEST段階で取得した減衰情報を保存するキャッシュ
    private static final Map<UUID, FoodDecayTracker.DecayInfo> savedDecayInfo = new ConcurrentHashMap<>();

    /**
     * NeoForgeイベントバスにハンドラを登録する。
     * Nutritional Balance MODがロードされている場合のみ呼び出すこと。
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(NutritionalBalanceIntegrationNeoForge.class);
    }

    /**
     * NB処理前（HIGHEST優先度）: 各栄養素の現在値をスナップショットとして保存する。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeNutritionalBalance(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().has(DataComponents.FOOD)) return;

        // WickerBasketはダミーのFoodPropertiesを持つが、実際の食べ物ではない。
        // 実際の食べ物はModCompatHelperNeoForgeで別途イベント発火されるため、バスケット自体のイベントはスキップ。
        if (event.getItem().getItem() instanceof WickerBasketItem) return;

        // FoodDecayTrackerの減衰情報をこの段階で先行保存する。
        FoodDecayTracker.DecayInfo decayInfo = FoodDecayTracker.get(player);
        if (decayInfo != null) {
            savedDecayInfo.put(player.getUUID(), decayInfo);
        }

        try {
            PlayerNutritionData worldData = PlayerNutritionData.getWorldNutritionData();
            if (worldData == null) return;

            INutritionalBalancePlayer nbPlayer = worldData.getNutritionalBalancePlayer(player);
            if (nbPlayer == null) return;

            Map<String, Float> snapshot = new HashMap<>();
            for (IPlayerNutrient nutrient : nbPlayer.getPlayerNutrients()) {
                snapshot.put(nutrient.getNutrientName(), nutrient.getValue());
            }
            preProcessValues.put(player.getUUID(), snapshot);
        } catch (Exception e) {
            // NB未初期化等のエラーは無視
        }
    }

    /**
     * NB処理後（LOWEST優先度）: スナップショットと現在値を比較し、
     * NBが加算した分に減衰倍率を適用する。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterNutritionalBalance(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Map<String, Float> preValues = preProcessValues.remove(player.getUUID());
        if (preValues == null) return;

        // HIGHESTで先行保存した減衰情報を使用する。
        FoodDecayTracker.DecayInfo info = savedDecayInfo.remove(player.getUUID());
        if (info == null) return;

        float multiplier = info.multiplier();
        // 減衰なし（倍率100%）の場合は調整不要
        if (multiplier >= 1.0f) return;

        try {
            PlayerNutritionData worldData = PlayerNutritionData.getWorldNutritionData();
            if (worldData == null) return;

            INutritionalBalancePlayer nbPlayer = worldData.getNutritionalBalancePlayer(player);
            if (nbPlayer == null) return;

            for (IPlayerNutrient nutrient : nbPlayer.getPlayerNutrients()) {
                Float preValue = preValues.get(nutrient.getNutrientName());
                if (preValue == null) continue;

                float added = nutrient.getValue() - preValue;
                if (added > 0) {
                    // NBが加算した分のうち、減衰で失われるべき量を減算
                    // 例: added=10, multiplier=0.5 → excess=5 → 最終加算量=5
                    float excess = added * (1.0f - multiplier);
                    nutrient.changeValue(-excess);
                }
            }
            worldData.setDirty();
        } catch (Exception e) {
            // NB未初期化等のエラーは無視
        }
    }
}
