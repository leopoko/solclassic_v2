package com.github.leopoko.solclassic.commands;

import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class ResetFoodHistoryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("resetfoodhistory")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    for (ServerPlayer player : targets) {
                                        FoodHistoryHolder.INSTANCE.resetFoodHistory(player);
                                    }
                                    // 成功メッセージを送信（管理者向け）
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Reset food history for " + targets.size() + " player(s)."),
                                            true
                                    );
                                    return targets.size();
                                }))
        );
    }
}
