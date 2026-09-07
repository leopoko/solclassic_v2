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
     * world/serverconfig/solclassic-server.toml を読み込み、SolClassicConfig の値を更新します。
     * toAbsolutePath() で絶対パスに変換することで、起動スクリプトのカレントディレクトリ差異を吸収します。
     */
    public static void loadConfig(MinecraftServer server) {
        try {
            Path configDir = server.getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath()
                    .resolve("serverconfig");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Path configFile = configDir.resolve(CONFIG_FILE_NAME);
            server.sendSystemMessage(Component.literal("[SolClassic] Config file path: " + configFile));

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
            // getLong/getDouble は TOML の値が期待した型と厳密に一致しない場合
            // （例: "1" のような整数リテラルを longFoodDecayModifiers に書いた場合）
            // null ではなく TomlInvalidTypeException を投げる。catch (Exception e) 自体は
            // 既にあるため起動クラッシュや完全な無反応にはならないが、例外発生時点で
            // メソッドが中断し、スカラー値も配列値も一切適用されないままになる
            // （＝有効な他の設定まで巻き込まれて無視される）。getNumber() 経由で
            // 数値型を許容してから doubleValue()/longValue() で変換し、この経路自体を防ぐ。
            Long maxFoodHistoryVal = toLong(getNumber(settings, "maxFoodHistorySize"));
            Long maxShortFoodHistoryVal = toLong(getNumber(settings, "maxShortFoodHistorySize"));
            Double longFoodDecayModifiersVal = toDouble(getNumber(settings, "longFoodDecayModifiers"));
            Boolean enableWickerBasket = settings.getBoolean("enableWickerBasket");
            Boolean guaranteeMinimumNutrition = settings.getBoolean("guaranteeMinimumNutrition");
            Boolean enableTooltip = settings.getBoolean("enableTooltip");
            Boolean enableItemDescription = settings.getBoolean("enableItemDescription");

            if (maxFoodHistoryVal == null || maxShortFoodHistoryVal == null || longFoodDecayModifiersVal == null || enableWickerBasket == null || guaranteeMinimumNutrition == null || enableTooltip == null || enableItemDescription == null) {
                server.sendSystemMessage(Component.literal("Invalid config keys detected. Recreating default config."));
                recreateDefaultConfig(server, configFile);
                result = Toml.parse(configFile);
                settings = result.getTable("SolClassicSettings");
                maxFoodHistoryVal = toLong(getNumber(settings, "maxFoodHistorySize"));
                maxShortFoodHistoryVal = toLong(getNumber(settings, "maxShortFoodHistorySize"));
                longFoodDecayModifiersVal = toDouble(getNumber(settings, "longFoodDecayModifiers"));
                enableWickerBasket = settings.getBoolean("enableWickerBasket");
                guaranteeMinimumNutrition = settings.getBoolean("guaranteeMinimumNutrition");
                enableTooltip = settings.getBoolean("enableTooltip");
                enableItemDescription = settings.getBoolean("enableItemDescription");
            }

            // スカラー値を先に適用（配列パース失敗時もスカラー値は反映される）
            SolclassicConfigData.maxFoodHistorySize = maxFoodHistoryVal.intValue();
            SolclassicConfigData.maxShortFoodHistorySize = maxShortFoodHistoryVal.intValue();
            SolclassicConfigData.longFoodDecayModifiers = longFoodDecayModifiersVal.floatValue();
            SolclassicConfigData.enableWickerBasket = enableWickerBasket;
            SolclassicConfigData.guaranteeMinimumNutrition = guaranteeMinimumNutrition;
            SolclassicConfigData.enableTooltip = enableTooltip;
            SolclassicConfigData.enableItemDescription = enableItemDescription;

            // shortFoodDecayModifiers は List<Double> として取得（nullチェック必須）
            if (settings.getArray("shortFoodDecayModifiers") != null) {
                List<Object> rawList = settings.getArray("shortFoodDecayModifiers").toList();
                if (rawList != null) {
                    SolclassicConfigData.shortFoodDecayModifiers = rawList.stream()
                            .map(o -> ((Number) o).floatValue())
                            .collect(Collectors.toList());
                }
            }

            if (settings.getArray("foodBlacklist") != null) {
                List<Object> foodBlacklist = settings.getArray("foodBlacklist").toList();
                if (foodBlacklist != null) {
                    SolclassicConfigData.foodBlacklist = foodBlacklist.stream()
                            .map(Object::toString).collect(Collectors.toList());
                }
            }

            // basketBlacklist は後から追加されたキーのため、既存の設定ファイルには存在しない。
            // その場合はハードコードデフォルト（空リスト）のままにする。
            if (settings.getArray("basketBlacklist") != null) {
                List<Object> basketBlacklist = settings.getArray("basketBlacklist").toList();
                if (basketBlacklist != null) {
                    SolclassicConfigData.basketBlacklist = basketBlacklist.stream()
                            .map(Object::toString).collect(Collectors.toList());
                }
            }

            server.sendSystemMessage(Component.literal("[SolClassic] Config loaded successfully."));

        } catch (Exception e) {
            server.sendSystemMessage(Component.literal("[SolClassic] Failed to load config: " + e.getMessage()));
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

    /**
     * TOML の数値キーを Number として取得します。
     * TomlTable#getDouble/getLong は値が期待した型(Double/Long)と厳密に一致しない場合、
     * null を返さずに TomlInvalidTypeException を投げるため直接は使いません。
     */
    private static Number getNumber(TomlTable settings, String key) {
        Object value = settings.get(key);
        return value instanceof Number number ? number : null;
    }

    private static Long toLong(Number number) {
        return number == null ? null : number.longValue();
    }

    private static Double toDouble(Number number) {
        return number == null ? null : number.doubleValue();
    }
}
