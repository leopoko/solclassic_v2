package com.github.leopoko.solclassic.utils;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 減衰で満腹度回復量が0になった食べ物を食べられないようにする (#57)。
 * <p>
 * 回復量0でも食べること自体はできたため、金リンゴ等を「効果だけ目当て」で食べ続けられた。
 * 右クリック時点 (Item.use より前) で両サイド共通に判定し、食べ始めをキャンセルする。
 * 判定に使う食事履歴と設定はどちらもクライアントへ同期されているため、両サイドの結果は一致する。
 */
public final class DecayedFoodGuard {

    private DecayedFoodGuard() {}

    public static void register() {
        InteractionEvent.RIGHT_CLICK_ITEM.register(DecayedFoodGuard::onRightClickItem);
    }

    private static CompoundEventResult<ItemStack> onRightClickItem(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isFullyDecayed(stack, player)) {
            return CompoundEventResult.pass();
        }
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.solclassic.food_fully_decayed"), true);
        }
        return CompoundEventResult.interruptFalse(stack);
    }

    /**
     * この食べ物を今食べても満腹度が一切回復しないかどうか。
     * ブラックリスト食品・クリエイティブ・最低回復保証が有効な場合は常に false。
     */
    public static boolean isFullyDecayed(ItemStack stack, Player player) {
        if (!SolclassicConfigData.preventEatingFullyDecayedFood) return false;
        if (SolclassicConfigData.guaranteeMinimumNutrition) return false;
        if (stack.isEmpty() || player.getAbilities().instabuild) return false;
        // WickerBasket は中身の食べ物から回復量 > 0 のものしか選ばないため対象外
        if (stack.getItem() instanceof WickerBasketItem) return false;
        if (!stack.has(DataComponents.FOOD)) return false;

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (SolclassicConfigData.foodBlacklist.contains(itemId)) return false;

        int baseNutrition = FoodHistoryHolder.INSTANCE.getEffectiveNutrition(stack, player);
        // 元から満腹度を回復しない食べ物（効果専用の食べ物など）は減衰とは無関係なので止めない
        if (baseNutrition <= 0) return false;

        float multiplier = FoodCalculator.CalculateMultiplier(stack, player);
        return FoodCalculator.CalculateNutrition(baseNutrition, multiplier) <= 0;
    }
}
