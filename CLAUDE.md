# CLAUDE.md - Spice of Life: Classic Edition

## 言語について / Language

**このリポジトリでの応答・コミットメッセージ・コメントはすべて日本語で行うこと。**
ソースコード内のコメントも日本語が使われているため、それに合わせること。

## プロジェクト概要

**Spice of Life: Classic Edition** は Minecraft 1.20.1 向けの食事多様性MODです。同じ食べ物を繰り返し食べると満腹度の回復量が減少する仕組みを実装しています。

- **MOD ID**: `solclassic`
- **パッケージ**: `com.github.leopoko.solclassic`
- **ライセンス**: MIT
- **Minecraft バージョン**: 1.20.1
- **Java バージョン**: 17
- **ビルドシステム**: Gradle + Architectury Loom 1.9
- **クロスプラットフォーム**: Architectury API 9.2.14 (Fabric + Forge)

## リポジトリ構成

```
solclassic_v2/
├── build.gradle              # ルートビルドファイル (Architectury プラグイン設定)
├── gradle.properties         # MODバージョン、依存関係バージョン定義
├── settings.gradle           # サブプロジェクト定義 (common, fabric, forge)
├── common/                   # 共通コード (プラットフォーム非依存)
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/github/leopoko/solclassic/
│       │   ├── Solclassic.java              # メイン初期化クラス
│       │   ├── commands/                     # コマンド
│       │   ├── config/                       # 設定データ
│       │   ├── container/                    # コンテナ/メニュー UI
│       │   ├── item/                         # アイテム (Basket, WickerBasket)
│       │   ├── network/                      # ネットワーク同期
│       │   └── utils/                        # ユーティリティ (FoodCalculator, Tooltip)
│       └── resources/
│           ├── solclassic.mixins.json
│           ├── assets/solclassic/            # テクスチャ、モデル、言語ファイル
│           └── data/solclassic/recipes/      # レシピ定義
├── fabric/                   # Fabric プラットフォーム固有コード
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/github/leopoko/solclassic/fabric/
│       │   ├── SolclassicFabric.java         # Fabric エントリポイント
│       │   ├── client/                        # クライアント初期化
│       │   ├── config/                        # TOML設定ローダー
│       │   ├── mixin/                         # Fabric用Mixin
│       │   ├── network/                       # FoodEventHandler実装
│       │   └── foodhistory/                   # Cardinal Components による履歴保存
│       └── resources/
│           ├── fabric.mod.json
│           └── solclassic-fabric.mixins.json
└── forge/                    # Forge プラットフォーム固有コード
    ├── build.gradle
    ├── gradle.properties
    └── src/main/
        ├── java/com/github/leopoko/solclassic/forge/
        │   ├── SolclassicForge.java           # Forge エントリポイント (@Mod)
        │   ├── config/                         # ForgeConfigSpec による設定
        │   ├── mixin/                          # Forge用Mixin + 競合解消プラグイン
        │   ├── network/                        # FoodEventHandler実装
        │   ├── foodhistory/                    # NBTベースの履歴永続化
        │   └── integration/                    # AppleSkin連携
        └── resources/
            ├── META-INF/mods.toml
            ├── solclassic-forge.mixins.json
            └── phantasm.mixins.json

```

## アーキテクチャ

### Architectury パターン

このMODは **Architectury API** を使用してFabricとForgeの両プラットフォームに対応しています。

- **`common/`**: プラットフォーム非依存の共有コード。ゲームロジック、アイテム定義、ネットワークインターフェースを含む
- **`fabric/`**: Fabric固有の実装。Cardinal Components APIで食事履歴を永続化
- **`forge/`**: Forge固有の実装。ForgeConfigSpec + NBT PersistentData で食事履歴を永続化

**重要**: `common/` のコードから直接 Fabric/Forge 固有のAPIを呼び出してはいけません。`IFoodEventHandler` インターフェースと `FoodHistoryHolder.INSTANCE` パターンでプラットフォーム差異を吸収しています。

### コア機能の仕組み

1. **食事履歴追跡**: プレイヤーが食べ物を食べるたびにMixin (`PlayerMixin`) が `Player.eat()` をインターセプトし、履歴に記録
2. **減衰計算** (`FoodCalculator`):
   - **短期減衰**: 直近 n 回の食事で同じ食べ物を食べた回数に基づく (`shortFoodDecayModifiers` リスト)
   - **長期減衰**: 全履歴での出現回数 × `longFoodDecayModifiers` (デフォルト 0.01)
   - 最終倍率 = `短期倍率 - 長期減衰` (最小0.0)
3. **アイテム消費リダイレクト**: `LivingEntityMixin` が `ItemStack.shrink()` をリダイレクトし、WickerBasket使用時はバスケット内の食べ物を消費
4. **ネットワーク同期**: サーバー側の食事履歴を `SyncFoodHistoryPacket` でクライアントに同期

### 主要クラスの役割

| クラス | 役割 |
|--------|------|
| `Solclassic` | 共通初期化 (アイテム登録、コマンド登録) |
| `FoodCalculator` | 食事減衰倍率の計算ロジック |
| `WickerBasketItem` | 最も栄養価の高い食べ物を自動選択して食べるアイテム |
| `BasketItem` | 9スロットの食べ物専用コンテナアイテム |
| `FoodContainer` / `FoodSlot` | 食べ物のみ受け付けるカスタムコンテナ/スロット |
| `IFoodEventHandler` | プラットフォーム共通の食事履歴操作インターフェース |
| `FoodHistoryHolder` | 食事イベントハンドラのシングルトンホルダー |
| `ClientTooltipHandler` | 食べ物ツールチップに減衰率を表示 |
| `PlayerMixin{Fabric,Forge}` | `Player.eat()` の栄養値を減衰に基づいて変更 |
| `LivingEntityMixin{Fabric,Forge}` | WickerBasket使用時のアイテム消費処理 |

## ビルドとテスト

### ビルドコマンド

```bash
# 全プラットフォームビルド
./gradlew build

# Fabric のみ
./gradlew :fabric:build

# Forge のみ
./gradlew :forge:build

# クライアント実行 (Fabric)
./gradlew :fabric:runClient

# クライアント実行 (Forge)
./gradlew :forge:runClient
```

### Gradle プロパティ (gradle.properties)

```properties
mod_version=2.01
minecraft_version=1.20.1
architectury_api_version=9.2.14
fabric_loader_version=0.16.10
fabric_api_version=0.92.3+1.20.1
forge_version=1.20.1-47.4.0
```

バージョンを変更する場合は `gradle.properties` を編集すること。

## 設定システム

### Fabric
- **ファイル**: `solclassic-server.toml` (サーバー起動時に `config/` ディレクトリに生成)
- **ローダー**: `SolClassicConfigLoaderFabric` (tomlj ライブラリ使用)
- サーバー起動イベント (`SERVER_STARTED`) で読み込み

### Forge
- **仕組み**: `ForgeConfigSpec` (`SolClassicConfigForge`)
- **タイプ**: `ModConfig.Type.SERVER`
- Mixin内で `SolClassicConfigInitForge.init()` を呼び出して共通設定データに反映

### 設定項目

| 項目 | デフォルト値 | 範囲 | 説明 |
|------|-------------|------|------|
| `maxFoodHistorySize` | 100 | 5-300 | 食事履歴の最大保存件数 |
| `maxShortFoodHistorySize` | 5 | 1-100 | 短期履歴の参照件数 |
| `longFoodDecayModifiers` | 0.01 | 0.0-1.0 | 長期減衰係数 |
| `shortFoodDecayModifiers` | [1.0, 0.9, 0.75, 0.5, 0.05] | — | 短期減衰係数リスト |
| `foodBlacklist` | ["minecraft:dried_kelp"] | — | 履歴追跡対象外の食べ物 |
| `enableWickerBasket` | true | — | WickerBasket機能の有効/無効 |

## 開発時の注意事項

### Mixin について
- Mixin は `priority = 1100` で設定されている（他MODとの競合を避けるため）
- `DisableConflictingMixinPlugin` で Phantasm MOD の `LivingEntityMixin` との競合を解消
- Fabric と Forge で別々の Mixin 実装がある（ロジックは同等だが API が異なる）

### コードを変更する際のガイドライン
1. **共通ロジック**は必ず `common/` モジュールに配置する
2. **プラットフォーム固有コード**は `fabric/` または `forge/` に配置する
3. `common/` からプラットフォーム固有 API にアクセスする場合は `IFoodEventHandler` のようなインターフェースパターンを使用する
4. 新しい食べ物関連の計算ロジックは `FoodCalculator` に追加する
5. 新しいアイテムは `Solclassic.init()` 内の `DeferredRegister` で登録する
6. ネットワークパケットは `ModNetworking` に登録する

### 食事履歴の永続化
- **Fabric**: Cardinal Components API (`IFoodHistoryComponentFabric`) — プレイヤーデータに自動保存、死亡時もコピー (`ALWAYS_COPY`)
- **Forge**: NBT PersistentData (`FoodHistoryManagerForge`) — プレイヤーログイン/ログアウトイベントで明示的に保存/読み込み

### 翻訳/ローカライズ
- 言語ファイルは `common/src/main/resources/assets/solclassic/lang/` にある
- 現在サポート: `en_us.json` (英語)、`ja_jp.json` (日本語)

### MOD連携
- **AppleSkin** (Forge): `AppleSkinEventHandler` で減衰後の栄養値を表示
- **JEI**: ランタイム依存として含まれている (レシピ表示用)
- **Phantasm**: Mixin競合を `DisableConflictingMixinPlugin` で回避

## コマンド

| コマンド | 説明 |
|---------|------|
| `/resetfoodhistory <players>` | 指定プレイヤーの食事履歴をリセット |
