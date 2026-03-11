package com.github.leopoko.solclassic.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * グローバルデフォルト設定。
 * modpack制作者が config/solclassic-defaults.toml を配置することで、
 * 新規ワールド作成時のサーバーコンフィグのデフォルト値を変更できる。
 * このファイルは自動生成されない（/solclassic generatedefaults コマンドで生成可能）。
 */
public class SolclassicGlobalDefaults {

    private static final Logger LOGGER = Logger.getLogger("SolClassic");
    public static final String DEFAULTS_FILE_NAME = "solclassic-defaults.toml";

    // デフォルト値（SolclassicConfigData と同じハードコード値で初期化）
    public int maxFoodHistorySize = 100;
    public int maxShortFoodHistorySize = 5;
    public double longFoodDecayModifiers = 0.01;
    public List<Double> shortFoodDecayModifiers = Arrays.asList(1.0, 0.9, 0.75, 0.5, 0.05);
    public List<String> foodBlacklist = Arrays.asList("minecraft:dried_kelp");
    public boolean enableWickerBasket = true;
    public boolean guaranteeMinimumNutrition = false;
    public boolean enableTooltip = true;
    public boolean enableItemDescription = true;

    /**
     * config ディレクトリから solclassic-defaults.toml を読み込む。
     * ファイルが存在しない場合はハードコードデフォルト値を返す。
     * 部分的な設定をサポート（指定されていないキーはフォールバック）。
     */
    public static SolclassicGlobalDefaults load(Path configDir) {
        SolclassicGlobalDefaults defaults = new SolclassicGlobalDefaults();

        Path globalConfigPath = configDir.resolve(DEFAULTS_FILE_NAME);
        if (!Files.exists(globalConfigPath)) {
            return defaults;
        }

        try {
            List<String> lines = Files.readAllLines(globalConfigPath, StandardCharsets.UTF_8);
            defaults.parseToml(lines);
            LOGGER.info("SolClassic: グローバルデフォルト設定を読み込みました: " + globalConfigPath);
        } catch (IOException e) {
            LOGGER.warning("SolClassic: グローバルデフォルト設定の読み込みに失敗しました: " + e.getMessage());
        }

        return defaults;
    }

    /**
     * 簡易TOMLパーサー。フラットなキーバリュー構造のみサポート。
     */
    private void parseToml(List<String> lines) {
        for (String rawLine : lines) {
            String line = rawLine.trim();

            // コメント行・空行・セクションヘッダーをスキップ
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) {
                continue;
            }

            int equalsIndex = line.indexOf('=');
            if (equalsIndex < 0) {
                continue;
            }

            String key = line.substring(0, equalsIndex).trim();
            String value = line.substring(equalsIndex + 1).trim();

            try {
                switch (key) {
                    case "maxFoodHistorySize":
                        this.maxFoodHistorySize = Integer.parseInt(value);
                        break;
                    case "maxShortFoodHistorySize":
                        this.maxShortFoodHistorySize = Integer.parseInt(value);
                        break;
                    case "longFoodDecayModifiers":
                        this.longFoodDecayModifiers = Double.parseDouble(value);
                        break;
                    case "shortFoodDecayModifiers":
                        this.shortFoodDecayModifiers = parseDoubleList(value);
                        break;
                    case "foodBlacklist":
                        this.foodBlacklist = parseStringList(value);
                        break;
                    case "enableWickerBasket":
                        this.enableWickerBasket = Boolean.parseBoolean(value);
                        break;
                    case "guaranteeMinimumNutrition":
                        this.guaranteeMinimumNutrition = Boolean.parseBoolean(value);
                        break;
                    case "enableTooltip":
                        this.enableTooltip = Boolean.parseBoolean(value);
                        break;
                    case "enableItemDescription":
                        this.enableItemDescription = Boolean.parseBoolean(value);
                        break;
                    default:
                        // 未知のキーは無視
                        break;
                }
            } catch (Exception e) {
                LOGGER.warning("SolClassic: グローバルデフォルト設定のキー '" + key + "' のパースに失敗しました: " + e.getMessage());
            }
        }
    }

    /**
     * "[1.0, 0.9, 0.75]" 形式の文字列をDouble型リストに変換する。
     */
    private static List<Double> parseDoubleList(String value) {
        // 角括弧を除去
        String inner = value.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);

        List<Double> result = new ArrayList<>();
        for (String element : inner.split(",")) {
            String trimmed = element.trim();
            if (!trimmed.isEmpty()) {
                result.add(Double.parseDouble(trimmed));
            }
        }
        return result;
    }

    /**
     * '["minecraft:dried_kelp", "minecraft:rotten_flesh"]' 形式の文字列をString型リストに変換する。
     */
    private static List<String> parseStringList(String value) {
        String inner = value.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);

        List<String> result = new ArrayList<>();
        for (String element : inner.split(",")) {
            String trimmed = element.trim();
            // クォートを除去
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * SolclassicGlobalDefaults の値からサーバーコンフィグ用のTOML文字列を生成する。
     * Fabric側のデフォルトコンフィグ生成でも使用する。
     */
    public static String generateConfigContent(SolclassicGlobalDefaults defaults) {
        StringBuilder sb = new StringBuilder();
        sb.append("[SolClassicSettings]\n");
        sb.append("#Maximum number of food history entries to track\n");
        sb.append("#Range: 5 ~ 300\n");
        sb.append("maxFoodHistorySize = ").append(defaults.maxFoodHistorySize).append("\n");
        sb.append("#Maximum number of food short history entries to track\n");
        sb.append("#Range: 1 ~ 100\n");
        sb.append("maxShortFoodHistorySize = ").append(defaults.maxShortFoodHistorySize).append("\n");
        sb.append("#Long decay modifiers for food recovery\n");
        sb.append("#Range: 0.0 ~ 1.0\n");
        sb.append("longFoodDecayModifiers = ").append(defaults.longFoodDecayModifiers).append("\n");
        sb.append("#List of decay modifiers for food recovery, applied sequentially\n");
        sb.append("shortFoodDecayModifiers = ").append(formatDoubleList(defaults.shortFoodDecayModifiers)).append("\n");
        sb.append("#List of food items that should not be tracked\n");
        sb.append("foodBlacklist = ").append(formatStringList(defaults.foodBlacklist)).append("\n");
        sb.append("#Enable Wicker Basket\n");
        sb.append("enableWickerBasket = ").append(defaults.enableWickerBasket).append("\n");
        sb.append("#Guarantee minimum 1 nutrition even when decay reduces it to 0. When false, fully decayed food gives no nutrition.\n");
        sb.append("guaranteeMinimumNutrition = ").append(defaults.guaranteeMinimumNutrition).append("\n");
        sb.append("#Enable food decay tooltip on food items\n");
        sb.append("enableTooltip = ").append(defaults.enableTooltip).append("\n");
        sb.append("#Enable description tooltip on mod items (Wicker Basket, Food History Book)\n");
        sb.append("enableItemDescription = ").append(defaults.enableItemDescription);
        return sb.toString();
    }

    /**
     * 現在のサーバーコンフィグ値 (SolclassicConfigData) をグローバルデフォルトファイルとして書き出す。
     */
    public static void writeCurrentConfigAsDefaults(Path configDir) throws IOException {
        SolclassicGlobalDefaults defaults = new SolclassicGlobalDefaults();
        // 現在のサーバーコンフィグ値をコピー
        defaults.maxFoodHistorySize = SolclassicConfigData.maxFoodHistorySize;
        defaults.maxShortFoodHistorySize = SolclassicConfigData.maxShortFoodHistorySize;
        // float -> double 変換時の精度劣化を防ぐため、Float.toString()経由で変換する
        // 直接キャストすると 0.01F → 0.009999999776482582 のようになる
        defaults.longFoodDecayModifiers = Double.parseDouble(Float.toString(SolclassicConfigData.longFoodDecayModifiers));
        defaults.shortFoodDecayModifiers = new ArrayList<>();
        for (Float f : SolclassicConfigData.shortFoodDecayModifiers) {
            defaults.shortFoodDecayModifiers.add(Double.parseDouble(Float.toString(f)));
        }
        defaults.foodBlacklist = new ArrayList<>(SolclassicConfigData.foodBlacklist);
        defaults.enableWickerBasket = SolclassicConfigData.enableWickerBasket;
        defaults.guaranteeMinimumNutrition = SolclassicConfigData.guaranteeMinimumNutrition;
        defaults.enableTooltip = SolclassicConfigData.enableTooltip;
        defaults.enableItemDescription = SolclassicConfigData.enableItemDescription;

        StringBuilder sb = new StringBuilder();
        sb.append("# Spice of Life: Classic Edition - Global Default Settings\n");
        sb.append("# This file defines default values for server config when creating new worlds.\n");
        sb.append("# It does NOT affect existing world configs.\n");
        sb.append("# You can remove any entry you don't need (missing entries will fall back to hardcoded defaults).\n");
        sb.append("\n");
        sb.append(generateConfigContent(defaults));

        Path outputFile = configDir.resolve(DEFAULTS_FILE_NAME);
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String formatDoubleList(List<Double> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatStringList(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
