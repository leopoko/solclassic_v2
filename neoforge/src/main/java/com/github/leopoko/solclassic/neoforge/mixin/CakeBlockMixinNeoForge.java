package com.github.leopoko.solclassic.neoforge.mixin;

import com.github.leopoko.solclassic.utils.CakeEatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ケーキに食事減衰を適用する Mixin。
 *
 * CakeBlock.eat() は Player.eat(Level, ItemStack) を通らず
 * FoodData.eat(int, float) を直接呼ぶため、PlayerMixinNeoForge の @Redirect では
 * 捕捉できない。ここで個別に差し替える。
 */
@Mixin(value = CakeBlock.class, priority = 1100)
public class CakeBlockMixinNeoForge {

    @Redirect(
            method = "eat(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;eat(IF)V")
    )
    private static void solclassic$modifyCakeRestoration(FoodData instance, int nutrition, float saturation,
                                                         LevelAccessor level, BlockPos pos, BlockState state, Player player) {
        CakeEatHandler.eatCake(instance, nutrition, saturation, player);
    }
}
