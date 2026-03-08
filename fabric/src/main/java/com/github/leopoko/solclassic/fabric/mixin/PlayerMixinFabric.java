package com.github.leopoko.solclassic.fabric.mixin;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.FoodHistorySync;
import com.github.leopoko.solclassic.utils.FoodCalculator;
import com.github.leopoko.solclassic.utils.FoodDecayTracker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class PlayerMixinFabric {

    @Redirect(
            method = "eat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V"),
            remap = true
    )
    private void modifyFoodRestoration(FoodData instance, Item item, ItemStack itemStack) {
        Player player = (Player) (Object) this;

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

        if (itemId.equals("solclassic:wicker_basket")) {
            // 旧方式: バスケット自体がPlayer.eat()に渡された場合（互換性のため保持）
            WickerBasketItem wickerBasketItem = (WickerBasketItem) itemStack.getItem();
            itemStack = wickerBasketItem.getMostNutritiousFood(itemStack, player);
            itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        } else {
            // 委譲方式: WickerBasketItem.finishUsingItem()がfoodCopy.finishUsingItem()に委譲し、
            // その中でplayer.eat(foodCopy)が呼ばれた場合。
            // itemStackは既に実際の食べ物だが、player.getUseItem()でバスケット使用中かを検出。
            // （Fabric版では現在追加の処理は不要だが、将来のMOD連携のため検出ロジックを用意）
            ItemStack useItem = player.getUseItem();
            if (!useItem.isEmpty() && useItem.getItem() instanceof WickerBasketItem) {
                // 委譲方式ではitemStackは既に実際の食べ物なので追加の処理は不要
            }
        }

        for (String itemID_ : SolclassicConfigData.foodBlacklist) {
            if (itemId.equals(itemID_)) {
                // ブラックリスト食品: 減衰なしで栄養値をそのまま適用（Quality Food等の品質修正は反映）
                int nutrition_ = FoodHistoryHolder.INSTANCE.getEffectiveNutrition(itemStack, player);
                float saturation_ = FoodHistoryHolder.INSTANCE.getEffectiveSaturationModifier(itemStack, player);
                if (nutrition_ == 0 && saturation_ == 0f) return;
                instance.eat(nutrition_, saturation_);
                return;
            }
        }

        // shrink(1)が先に実行されるため、スタック数1の場合countが0になる。
        // getItem()はcount=0でも正しいアイテムを返すので、isEdible()で判定する。
        if (itemStack.getItem().isEdible()) {
            // Quality Food等のMODによる品質修正を反映した栄養値を取得
            int baseNutrition = FoodHistoryHolder.INSTANCE.getEffectiveNutrition(itemStack, player);
            float baseSaturation = FoodHistoryHolder.INSTANCE.getEffectiveSaturationModifier(itemStack, player);
            if (baseNutrition == 0 && baseSaturation == 0f) return;

            // count=0のItemStackでも正しく比較できるが、履歴保存用にcount>=1のスタックを用意する
            ItemStack foodToRecord = itemStack.isEmpty() ? new ItemStack(itemStack.getItem()) : itemStack;

            float multiplier = FoodCalculator.CalculateMultiplier(foodToRecord, player);
            int nutrition = FoodCalculator.CalculateNutrition(baseNutrition, multiplier);

            // Diet MOD連携: 減衰倍率と実際の食べ物を保存（Dietのイベントハンドラで使用）
            FoodDecayTracker.recordDecay(player, itemStack, multiplier);

            instance.eat(nutrition, baseSaturation);

            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                FoodHistoryHolder.INSTANCE.addFoodHistory((ServerPlayer) player, foodToRecord, SolclassicConfigData.maxFoodHistorySize);
                FoodHistorySync.syncFoodHistory((ServerPlayer) player);
            }
        }
    }
}


