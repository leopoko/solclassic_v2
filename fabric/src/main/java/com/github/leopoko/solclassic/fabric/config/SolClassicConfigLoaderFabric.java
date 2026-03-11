package com.github.leopoko.solclassic.fabric.config;

import com.github.leopoko.solclassic.config.SolclassicConfigData;
import com.github.leopoko.solclassic.config.SolclassicGlobalDefaults;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class SolClassicConfigLoaderFabric {

    public static final String CONFIG_FILE_NAME = "solclassic-server.toml";

    /**
     * サーバー（ワールド）起動時に呼ばれる処理です。
     * config/ 配下の solclassic-server.toml を読み込み、SolClassicConfig の値を更新します。
     */
    public static void loadConfig(MinecraftServer server) {
        try {
            // サーバーのルートディレクトリから config フォルダを取得
            Path configDir = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Path configFile = configDir.resolve(CONFIG_FILE_NAME);

            // ファイルが存在しなければ、デフォルトの内容を書き込む
            if (!Files.exists(configFile)) {
                recreateDefaultConfig(server, configFile);
            }

            // TOML パース
            TomlParseResult result = Toml.parse(configFile);

            if (!result.errors().isEmpty() || result.getTable("SolClassicSettings") == null) {
                server.sendSystemMessage(Component.literal("Config file is invalid. Recreating default config at: " + configFile));
                recreateDefaultConfig(server, configFile);
                result = Toml.parse(configFile);
            }

            // [SolClassicSettings] テーブルがある前提
            TomlTable settings = result.getTable("SolClassicSettings");
            if (settings == null) {
                server.sendSystemMessage(Component.literal("[SolClassicSettings] table not found in config"));
                return;
            }

            // 各値を取得（null の場合は再作成する）
            Long maxFoodHistoryVal = settings.getLong("maxFoodHistorySize");
            Long maxShortFoodHistoryVal = settings.getLong("maxShortFoodHistorySize");
            Double longFoodDecayModifiersVal = settings.getDouble("longFoodDecayModifiers");
            Boolean enableWickerBasket = settings.getBoolean("enableWickerBasket");
            Boolean guaranteeMinimumNutrition = settings.getBoolean("guaranteeMinimumNutrition");
            Boolean enableTooltip = settings.getBoolean("enableTooltip");
            Boolean enableItemDescription = settings.getBoolean("enableItemDescription");

            if (maxFoodHistoryVal == null || maxShortFoodHistoryVal == null || longFoodDecayModifiersVal == null || enableWickerBasket == null || guaranteeMinimumNutrition == null || enableTooltip == null || enableItemDescription == null) {
                server.sendSystemMessage(Component.literal("Invalid config keys detected. Recreating default config."));
                recreateDefaultConfig(server, configFile);
                result = Toml.parse(configFile);
                settings = result.getTable("SolClassicSettings");
                maxFoodHistoryVal = settings.getLong("maxFoodHistorySize");
                maxShortFoodHistoryVal = settings.getLong("maxShortFoodHistorySize");
                longFoodDecayModifiersVal = settings.getDouble("longFoodDecayModifiers");
                enableWickerBasket = settings.getBoolean("enableWickerBasket");
                guaranteeMinimumNutrition = settings.getBoolean("guaranteeMinimumNutrition");
                enableTooltip = settings.getBoolean("enableTooltip");
                enableItemDescription = settings.getBoolean("enableItemDescription");
            }

            // shortFoodDecayModifiers は List<Double> として取得（値は Number 型なので変換する）
            List<Object> rawList = settings.getArray("shortFoodDecayModifiers").toList();
            if (rawList != null) {
                SolclassicConfigData.shortFoodDecayModifiers = rawList.stream()
                        .map(o -> ((Number) o).floatValue())
                        .collect(Collectors.toList());
            }

            List<Object> foodBlacklist = settings.getArray("foodBlacklist").toList();
            if (foodBlacklist != null) {
                SolclassicConfigData.foodBlacklist = foodBlacklist.stream()
                        .map(Object::toString).collect(Collectors.toList());
            }

            server.sendSystemMessage(Component.literal("SolClassic config loaded successfully."));

            SolclassicConfigData.maxFoodHistorySize = maxFoodHistoryVal.intValue();
            SolclassicConfigData.maxShortFoodHistorySize = maxShortFoodHistoryVal.intValue();
            SolclassicConfigData.longFoodDecayModifiers = longFoodDecayModifiersVal.floatValue();
            SolclassicConfigData.enableWickerBasket = enableWickerBasket;
            SolclassicConfigData.guaranteeMinimumNutrition = guaranteeMinimumNutrition;
            SolclassicConfigData.enableTooltip = enableTooltip;
            SolclassicConfigData.enableItemDescription = enableItemDescription;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fabric のサーバー起動イベントに登録するためのメソッド。
     * このメソッドを Mod の初期化時に呼び出してください。
     */
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(SolClassicConfigLoaderFabric::loadConfig);
    }

    private static void recreateDefaultConfig(MinecraftServer server, Path configFile) throws IOException {
        // グローバルデフォルト設定を読み込み、その値でサーバーコンフィグを生成
        SolclassicGlobalDefaults defaults = SolclassicGlobalDefaults.load(
                FabricLoader.getInstance().getConfigDir());
        String content = SolclassicGlobalDefaults.generateConfigContent(defaults);
        Files.writeString(configFile, content, StandardCharsets.UTF_8);
        server.sendSystemMessage(Component.literal("Default config recreated at: " + configFile));
    }
}
