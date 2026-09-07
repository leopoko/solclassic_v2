package com.github.leopoko.solclassic.utils;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.FoodHistorySync;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * ケーキ（設置ブロックとして food する食べ物）に減衰を適用するための共通処理。
 *
 * <p>{@code CakeBlock.eat()} は {@code Player.eat(Level, ItemStack)} を経由せず、
 * {@code FoodData.eat(int, float)} を直接呼ぶ。そのため PlayerMixin の
 * {@code @Redirect} が発火せず、減衰も履歴記録も行われなかった。
 * CakeBlockMixin から本メソッドを呼び出すことで、通常の食事と同じ扱いにする。</p>
 *
 * <p>バニラ 1.21.1 で {@code FoodData.eat(int, float)} を直接呼ぶ食べ物ブロックは
 * {@code CakeBlock} のみ（キャンドルケーキも同じ {@code CakeBlock.eat} を経由する）。</p>
 */
public final class CakeEatHandler {

    private CakeEatHandler() {
    }

    /**
     * {@code CakeBlock.eat()} 内の {@code FoodData.eat(int, float)} を置き換える処理。
     *
     * @param foodData   対象プレイヤーの FoodData
     * @param nutrition  バニラが渡してくる満腹度回復量（ケーキは 2）
     * @param saturation バニラが渡してくる隠し満腹度（ケーキは 0.1F）
     * @param player     ケーキを食べたプレイヤー
     */
    public static void eatCake(FoodData foodData, int nutrition, float saturation, Player player) {
        if (player == null) {
            foodData.eat(nutrition, saturation);
            return;
        }

        ItemStack cake = new ItemStack(Items.CAKE);
        String itemId = BuiltInRegistries.ITEM.getKey(Items.CAKE).toString();

        // ブラックリスト食品: 減衰も履歴記録も行わず、バニラどおりに回復させる
        if (SolclassicConfigData.foodBlacklist != null && SolclassicConfigData.foodBlacklist.contains(itemId)) {
            foodData.eat(nutrition, saturation);
            return;
        }

        // 隠し満腹度は他の食べ物と同様に減衰させない（PlayerMixin と同じ扱い）
        float multiplier = FoodCalculator.CalculateMultiplier(cake, player);
        foodData.eat(FoodCalculator.CalculateNutrition(nutrition, multiplier), saturation);

        if (player instanceof ServerPlayer serverPlayer) {
            FoodHistoryHolder.INSTANCE.addFoodHistory(serverPlayer, cake, SolclassicConfigData.maxFoodHistorySize);
            FoodHistorySync.syncFoodHistory(serverPlayer);
        }
    }
}
