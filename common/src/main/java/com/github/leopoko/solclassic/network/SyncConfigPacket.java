package com.github.leopoko.solclassic.network;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * サーバー設定 (SolclassicConfigData) をクライアントへ同期するパケット。
 * <p>
 * Fabric 版の設定はサーバーのワールドフォルダ (serverconfig/solclassic-server.toml) からしか読まれず、
 * 専用サーバーではクライアント側の SolclassicConfigData がデフォルト値のままだった。
 * その結果、ツールチップの減衰率・AppleSkin の表示・Wicker Basket の自動選択がサーバー設定と一致しなかった。
 * Forge は SERVER 設定を自前で同期するが、同じ値を上書きするだけなので両プラットフォームで送ってよい。
 */
public class SyncConfigPacket {

    private final int maxFoodHistorySize;
    private final int maxShortFoodHistorySize;
    private final float longFoodDecayModifiers;
    private final List<Float> shortFoodDecayModifiers;
    private final List<String> foodBlacklist;
    private final List<String> basketBlacklist;
    private final boolean enableWickerBasket;
    private final boolean guaranteeMinimumNutrition;
    private final boolean preventEatingFullyDecayedFood;
    private final boolean enableTooltip;
    private final boolean enableItemDescription;

    /** 現在のサーバー設定から作成する */
    public SyncConfigPacket() {
        this.maxFoodHistorySize = SolclassicConfigData.maxFoodHistorySize;
        this.maxShortFoodHistorySize = SolclassicConfigData.maxShortFoodHistorySize;
        this.longFoodDecayModifiers = SolclassicConfigData.longFoodDecayModifiers;
        this.shortFoodDecayModifiers = new ArrayList<>(SolclassicConfigData.shortFoodDecayModifiers);
        this.foodBlacklist = new ArrayList<>(SolclassicConfigData.foodBlacklist);
        this.basketBlacklist = new ArrayList<>(SolclassicConfigData.basketBlacklist);
        this.enableWickerBasket = SolclassicConfigData.enableWickerBasket;
        this.guaranteeMinimumNutrition = SolclassicConfigData.guaranteeMinimumNutrition;
        this.preventEatingFullyDecayedFood = SolclassicConfigData.preventEatingFullyDecayedFood;
        this.enableTooltip = SolclassicConfigData.enableTooltip;
        this.enableItemDescription = SolclassicConfigData.enableItemDescription;
    }

    public SyncConfigPacket(FriendlyByteBuf buf) {
        this.maxFoodHistorySize = buf.readVarInt();
        this.maxShortFoodHistorySize = buf.readVarInt();
        this.longFoodDecayModifiers = buf.readFloat();
        this.shortFoodDecayModifiers = buf.readList(FriendlyByteBuf::readFloat);
        this.foodBlacklist = buf.readList(FriendlyByteBuf::readUtf);
        this.basketBlacklist = buf.readList(FriendlyByteBuf::readUtf);
        this.enableWickerBasket = buf.readBoolean();
        this.guaranteeMinimumNutrition = buf.readBoolean();
        this.preventEatingFullyDecayedFood = buf.readBoolean();
        this.enableTooltip = buf.readBoolean();
        this.enableItemDescription = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(maxFoodHistorySize);
        buf.writeVarInt(maxShortFoodHistorySize);
        buf.writeFloat(longFoodDecayModifiers);
        buf.writeCollection(shortFoodDecayModifiers, FriendlyByteBuf::writeFloat);
        buf.writeCollection(foodBlacklist, FriendlyByteBuf::writeUtf);
        buf.writeCollection(basketBlacklist, FriendlyByteBuf::writeUtf);
        buf.writeBoolean(enableWickerBasket);
        buf.writeBoolean(guaranteeMinimumNutrition);
        buf.writeBoolean(preventEatingFullyDecayedFood);
        buf.writeBoolean(enableTooltip);
        buf.writeBoolean(enableItemDescription);
    }

    public void applyToConfig() {
        SolclassicConfigData.maxFoodHistorySize = maxFoodHistorySize;
        SolclassicConfigData.maxShortFoodHistorySize = maxShortFoodHistorySize;
        SolclassicConfigData.longFoodDecayModifiers = longFoodDecayModifiers;
        SolclassicConfigData.shortFoodDecayModifiers = shortFoodDecayModifiers;
        SolclassicConfigData.foodBlacklist = foodBlacklist;
        SolclassicConfigData.basketBlacklist = basketBlacklist;
        SolclassicConfigData.enableWickerBasket = enableWickerBasket;
        SolclassicConfigData.guaranteeMinimumNutrition = guaranteeMinimumNutrition;
        SolclassicConfigData.preventEatingFullyDecayedFood = preventEatingFullyDecayedFood;
        SolclassicConfigData.enableTooltip = enableTooltip;
        SolclassicConfigData.enableItemDescription = enableItemDescription;
    }
}
