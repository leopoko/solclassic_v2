package com.github.leopoko.solclassic.commands;

import com.github.leopoko.solclassic.config.SolclassicGlobalDefaults;
import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;

public class GenerateDefaultsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("solclassic")
                        .then(Commands.literal("generatedefaults")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    return generateDefaults(context.getSource(), false);
                                })
                                .then(Commands.literal("force")
                                        .executes(context -> {
                                            return generateDefaults(context.getSource(), true);
                                        })))
        );
    }

    private static int generateDefaults(CommandSourceStack source, boolean force) {
        try {
            Path configDir = Platform.getConfigFolder();
            Path defaultsFile = configDir.resolve(SolclassicGlobalDefaults.DEFAULTS_FILE_NAME);

            if (Files.exists(defaultsFile) && !force) {
                source.sendFailure(Component.literal(
                        "グローバルデフォルト設定ファイルは既に存在します: " + defaultsFile +
                        "\n上書きするには /solclassic generatedefaults force を使用してください。"));
                return 0;
            }

            SolclassicGlobalDefaults.writeCurrentConfigAsDefaults(configDir);
            source.sendSuccess(
                    () -> Component.literal("グローバルデフォルト設定を書き出しました: " +
                            configDir.resolve(SolclassicGlobalDefaults.DEFAULTS_FILE_NAME)),
                    true
            );
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    "グローバルデフォルト設定の書き出しに失敗しました: " + e.getMessage()));
            return 0;
        }
    }
}
