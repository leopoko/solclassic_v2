# 1.21.1 移行担当AI向け 引き継ぎメモ

**最終更新**: 2026-09-08 / **対象**: `main` (1.20.1) → `1.21.1` ブランチへの変更移植

このドキュメントは `main` ブランチに入った変更のうち、`1.21.1` ブランチへ移植が必要なものと、
移植時に踏み抜きやすい API 差分をまとめたものです。

---

## 0. ブランチ状態

| ブランチ | HEAD | バージョン | プラットフォーム |
|---|---|---|---|
| `main` | `24e74f4` | `2.13` | Fabric + Forge |
| `1.21.1` | `361a20d` | `2.14+1.21.1` | Fabric + NeoForge |

### 移植状況サマリ

| # | 内容 | 由来 | 1.21.1 への移植 |
|---|---|---|---|
| 1 | Fabric TOML 数値リテラル修正 | PR #56 / `f12ff49` | **不要**（元から 1.21.1 向け） |
| 2 | レシピフォルダ `recipes` → `recipe` | Issue #54 / `361a20d` | **不要**（済） |
| 3 | 中国語ローカライズ | PR #53 / `706faba` | **要移植**（容易） |
| 4 | 消費回数キャッシュ | PR #55 `56aab2c` + 追い修正 `0a2409c` | **要移植**（要注意） |
| 5 | `basketBlacklist` 設定 | Issue #52 / `24e74f4` | **要移植** |
| 6 | ケーキの減衰対応 | Issue #3 / `24e74f4` | **要移植** |
| 7 | バスケットのスロット同期修正 | v2.11 / `1ccd9f8` | **要移植**（既存の未修正バグ） |

---

## 1. 移植不要なもの

### 1-1. Fabric 設定ローダーの数値リテラル対応（PR #56）

`1.21.1` ブランチに直接マージ済み。`getNumber()` / `toLong()` / `toDouble()` ヘルパーが既に入っています。

`TomlTable#getLong` / `getDouble` は TOML リテラルの型が厳密に一致しないと `null` ではなく
`TomlInvalidTypeException` を投げるため、`longFoodDecayModifiers = 1`（`1.0` ではなく整数リテラル）
と書かれた瞬間に `loadConfig()` が中断し、設定全体が無視される問題への対処です。

### 1-2. 配列キーの null チェック

`main` では `getList()` ヘルパーを追加しましたが、**1.21.1 側は既に対応済み**です
（v2.12+1.21.1 の修正で `if (settings.getArray("...") != null)` の形になっている）。
パターンは違いますが機能的に等価なので、`main` の `getList()` を移植する必要はありません。

ただし**新しい配列キー（`basketBlacklist`）を足す際は、既存2キーと同じ null チェックの形を必ず踏襲**してください。
`TomlTable#getArray` はキーが無いと `null` を返すため、チェックを忘れると既存の設定ファイルが
すべて読み込めなくなります。

---

## 2. 要移植: 中国語ローカライズ

- **対象**: `common/src/main/resources/assets/solclassic/lang/zh_cn.json`（新規）
- **内容**: `en_us.json` と同じ17キーの中国語訳
- **注意点**: 1.21.1 の `en_us.json` とキー構成が一致しているか確認すること。
  キーが増減していれば差分だけ追加・削除する。それ以外に注意点なし

---

## 3. 要移植: 食事履歴の消費回数キャッシュ

### 目的

長期減衰で使う `countFoodEaten()` が履歴全体（最大300件）を毎回線形走査していたのを、
`Map<Item, Integer>` のカウンタキャッシュで O(1) にする。

### 対象ファイル

- `common/utils/FoodHistory.java`（**新規**）
- `common/network/IFoodEventHandler.java` — `LinkedList<ItemStack>` を返していた3メソッドの戻り値/引数を `FoodHistory` に変更
- `common/network/{ClientPacketHandler, FoodHistorySync, SyncFoodHistoryPacket}.java`
- `common/client/FoodHistoryBookScreen.java`
- `fabric/foodhistory/{FoodHistoryComponentFabric, IFoodHistoryComponentFabric}.java`
- `fabric/network/FoodEventHandlerFabric.java`
- Forge 側 → **NeoForge 側の対応クラス**: `FoodHistoryManagerNeoForge` / `PlayerDataHandlerNeoForge` / `FoodEventHandlerNeoForge`

### 元PRのまま移植してはいけません

PR #55 を `main` にマージした後、以下4件を追加修正しています（`0a2409c`）。
**移植時は必ずセットで適用してください。**

#### (1) NPE — 最重要

`FoodEventHandlerForge.countFoodEatenRecent()` が null チェックより前に `.consumedItems` を参照していました。

```java
// 元PR（バグ）
LinkedList<ItemStack> history = foodHistories.get(player.getUUID()).consumedItems;
if (history == null || history.isEmpty()) { return 0; }
```

`/resetfoodhistory` は `foodHistories.remove(uuid)` するだけで再同期しないため、
リセット直後の食事で `PlayerMixin` → `FoodCalculator.CalculateMultiplier` → `ShortMultiplier` →
ここで確実にクラッシュします。

`FoodHistory` 自体の null を先に判定するよう修正し、
`countFoodEaten()` / `getClientFoodHistory()` にも `player == null` ガードを追加しました。

#### (2) 防御的コピーの復活

`FoodHistoryComponentFabric.setFood()` から `new LinkedList<>(newHistory)` が削除されていました。
あのコピーは**自己代入対策**として load-bearing で、無いと `setFood(自分自身のhistory)` が
`clear()` → 空をコピー、となり**履歴を全消去**します。

コピーを戻し、`newHistory == history` の明示ガードも追加。
`add(stack.copy())` 経由で入れ直すとカウンタキャッシュも同時に再構築されます。

#### (3) 例外でクラッシュさせない

`decrementConsumption()` がキューとキャッシュの不整合時に `IllegalStateException` を投げていました。
これは `Player.eat()` の内部（Mixin）で走るため、ゲーム/サーバーごと落ちます。
ログ出力 + `consumedItems` からのキャッシュ再構築、に変更。

#### (4) `toString()` の `%b` → `%s`

`FoodHistory:{Map: {true}, Queue: true}` としか出力されず、(3) の診断ログが無意味になっていました。
`StringJoiner` で書き直し済み。

#### その他

- `add(item, maxHistory)` に `maxHistory <= 0` ガード
- 上限超過分を `while` でまとめて切り詰め（`maxFoodHistorySize` を下げたとき即座に効く）
- インデントをタブ→スペース4に統一、コメントを日本語化

### 1.21.1 での注意点

- **`IFoodEventHandler` は 1.21.1 でも `LinkedList<ItemStack>` のまま**。
  この移植は同インターフェースのシグネチャを全面的に変える大きな変更になります
- **NBT 永続化の API が違います**:
  - `readFromNbt(CompoundTag, HolderLookup.Provider)` / `writeToNbt(CompoundTag, HolderLookup.Provider)`
  - `ItemStack.of(tag)` → `ItemStack.parseOptional(registryLookup, tag)`
  - `stack.save(tag)` → `(CompoundTag) stack.save(registryLookup)`
- **CCA のパッケージが違います**: `dev.onyxstudios.cca` → `org.ladysnake.cca`
- `FoodHistory` 本体は `BuiltInRegistries.ITEM` と `java.util.logging.Logger` しか使っておらず、
  **そのまま移植可能**
- カウンタとキューの整合性は「追加・削除を必ず `add()` / `clear()` 経由にする」ことで担保しています。
  `consumedItems` を直接 `add` / `remove` する実装を書かないこと

---

## 4. 要移植: `basketBlacklist` 設定

### 目的

Basket / Wicker Basket に入れられない食べ物を設定で指定できるようにする。
減衰追跡から外す `foodBlacklist` とは**目的が異なる別設定**。

### 対象ファイル

- `common/utils/BasketBlacklist.java`（**新規**）
- `common/config/SolclassicConfigData.java` — `basketBlacklist` フィールド（デフォルト空リスト）
- `common/config/SolclassicGlobalDefaults.java` — フィールド / `parseToml` の case /
  `generateConfigContent` / `writeCurrentConfigAsDefaults` の4箇所
- `common/container/FoodSlot.java` — `mayPlace()`
- `common/container/FoodContainer.java` — `canAccept()` ヘルパーに集約し
  `canPlaceItem` / `addItem` / `canAddItem` から呼ぶ
- `common/item/WickerBasketItem.java` — `getMostNutritiousFood()` でスキップ
- `SolClassicConfigNeoForge` / `SolClassicConfigInitNeoForge` / `SolclassicNeoForge.applyGlobalDefaults()`
- `fabric/config/SolClassicConfigLoaderFabric.java`

### 設計上いちばん重要な点: アイテム消失の防止

判定を入れるのは**「新規に入れる」経路だけ**です。

| 経路 | 判定 | 理由 |
|---|---|---|
| `FoodSlot.mayPlace()` | あり | GUI 配置・シフトクリック |
| `FoodContainer.canPlaceItem()` / `addItem()` / `canAddItem()` | あり | コンテナレベルの二重チェック |
| `FoodContainer.setItem()` | **入れてはいけない** | NBT 復元 (`container.fromTag()`) がここを通る。判定を入れると、後からブラックリストに追加した瞬間に既存バスケットの中身が消える |
| `WickerBasketItem.getMostNutritiousFood()` | 除外のみ | 既存分は取り出せるが自動で食べられない |

### 1.21.1 での注意点

- **`Item.isEdible()` は 1.21.1 に存在しません。**
  1.21.1 の `FoodSlot` / `FoodContainer` は既に `stack.has(DataComponents.FOOD)` を使っているので、
  `isFood()` はそちらの既存実装をそのまま使うこと
- **1.21.1 の `FoodContainer` には `canPlaceItem()` オーバーライドがありません**
  （`main` では v2.11 で追加済み、未移植）。`canAccept()` ヘルパーへの集約をそのまま持っていくなら、
  `canPlaceItem()` の追加も同時に行うことになります。これは下記 6. の移植と重なる作業です
- `BuiltInRegistries.ITEM.getKey()` は 1.21.1 でも同じ
- **`ModConfigSpec.defineList()` に空リストのデフォルトを渡す点は要確認**。
  `main` では `ForgeConfigSpec` + `Collections.emptyList()` でビルドが通ることを確認していますが、
  NeoForge の `ModConfigSpec` はバージョンによって `Supplier<List<?>>` を要求する
  `defineList` オーバーロードしか受け付けない場合があります。コンパイルエラーになったら
  `defineList("basketBlacklist", () -> Collections.emptyList(), o -> o instanceof String)` 形式を試すこと
- 生成される TOML は `basketBlacklist = []` になります。
  `SolclassicGlobalDefaults.parseStringList("[]")` が空リストを返すこと、
  キーが無い旧ファイルでデフォルトにフォールバックすることは `main` 側で実測確認済み

### 報告者への回答時の注意（Issue #52）

この設定は**バスケットに入れられなくするだけ**で、報告者（Mohist サーバーで Skript を使用）の
本来の目的は完全には解決しません。真因は、`WickerBasketItem.finishUsingItem()` が
`foodCopy.finishUsingItem()` に委譲するため、CraftBukkit の `PlayerItemConsumeEvent` が
実際の食べ物ではなく `solclassic:wicker_basket` に対して発火することです。

---

## 5. 要移植: ケーキの減衰対応

### 症状と原因

ケーキだけ減衰も履歴記録もされていませんでした。
MOD の `PlayerMixin` は `Player.eat()` 内の `FoodData.eat(...)` を `@Redirect` していますが、
`CakeBlock.eat()` は `Player.eat()` を**一切通らず** `FoodData.eat(int, float)` を直接呼びます。

```java
// CakeBlock.eat() — 1.20.1 / 1.21.1 とも同じ
arg4.awardStat(Stats.EAT_CAKE_SLICE);
arg4.getFoodData().eat(2, 0.1F);   // ← ここ
```

バニラ 1.20.1 の `net/minecraft/world` 配下を全走査し、`FoodData.eat(IF)V` を呼ぶのは
`CakeBlock` と `MobEffect`（満腹度エフェクト用・無関係）のみであることを確認済みです。
**バニラで漏れているのはケーキだけ**（キャンドルケーキも同じ `CakeBlock.eat()` を経由）。

他MODの食べ物ブロックは MOD ごとに経路が違うため、一律には対応できません。

### 対象ファイル

- `common/utils/CakeEatHandler.java`（**新規**、共通ロジック）
- `fabric/mixin/CakeBlockMixinFabric.java`（**新規**）
- `forge/mixin/CakeBlockMixinForge.java` → **`neoforge/mixin/CakeBlockMixinNeoForge.java`**
- 両 `mixins.json` への登録

### 1.21.1 での注意点

**1.21.1 でも `CakeBlock.eat` のシグネチャと `FoodData.eat(int, float)` は変わっていません**
（NeoForge 21.1.219 のマージ済み jar でバイトコード確認済み）。descriptor 指定込みでほぼそのまま移植できます。

```java
@Redirect(
    method = "eat(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V")
)
private static void solclassic$modifyCakeRestoration(FoodData instance, int nutrition, float saturation,
                                                     LevelAccessor level, BlockPos pos, BlockState state, Player player) {
    CakeEatHandler.eatCake(instance, nutrition, saturation, player);
}
```

**ただし `FoodData` の他のメソッドは変わっています。**
1.21.1 の `FoodData` は `eat(int, float)` と `eat(FoodProperties)` の2つで、
`eat(Item, ItemStack)` は**存在しません**。そのため 1.21.1 の `PlayerMixin` は
`FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V` を `@Redirect` し、
`@Inject(method="eat", at=@At("HEAD"))` で `ItemStack` を別途キャプチャする形になっています。
**ケーキ用 Mixin はこの `PlayerMixin` とはターゲットが別（`eat(IF)V`）なので干渉しません。**

その他:

- `Items.CAKE` / `BuiltInRegistries.ITEM` は 1.21.1 でも同じ
- `neoforge` の `mixins.json` は `compatibilityLevel: JAVA_21`、パッケージは
  `com.github.leopoko.solclassic.neoforge.mixin`。`mixins` 配列への追記を忘れないこと
- `CakeEatHandler` は `foodBlacklist` に `minecraft:cake` があれば減衰も履歴記録もせず
  バニラどおりに回復させます（他の食べ物と同じ扱い）
- 隠し満腹度（saturation）は減衰させていません。`PlayerMixin` の既存挙動に合わせています
- ビルド後は refmap でターゲット解決を確認すること。`main` では
  Forge `CakeBlock.m_51185_` / `FoodData.m_38707_(IF)V`、
  Fabric `class_2272.method_9719` / `class_1702.method_7585(IF)V` に解決されることを確認済み

---

## 6. 要移植: バスケットのスロット同期修正（Issue #51）

`main` は v2.11 でカスタム `MenuType` を登録して根本修正済みですが、
`1.21.1` は今も `MenuType.GENERIC_9x1` のままです。

```
1.21.1: BasketItem.java:51        new FoodChestMenu(MenuType.GENERIC_9x1, ...)
1.21.1: WickerBasketItem.java:114 new FoodChestMenu(MenuType.GENERIC_9x1, ...)
```

クライアントがバニラの `ChestMenu` + `SimpleContainer`（食べ物バリデーション無し）を生成し、
サーバーは `FoodChestMenu` + `FoodContainer` + `FoodSlot` を使う不一致が残っており、
**Issue #51（バスケットに約3個入れると他のアイテムが消える）が 1.21.1 で再現する状態**です。
v2.09 の `Slot.index` 対症療法は入っていますが、根本修正と
`FoodContainer.canPlaceItem()` オーバーライドはどちらも未移植です。

移植元:

- `main` の `Solclassic.FOOD_CHEST_MENU_TYPE`（Architectury `MenuRegistry.ofExtended()` で登録）
- `BasketItem` / `WickerBasketItem` の `MenuRegistry.openExtendedMenu()` 呼び出し
- `FoodContainer.canPlaceItem()`

**4. の `basketBlacklist` で `FoodContainer.canPlaceItem()` を触るので、
先にこの移植を済ませてからの方が手戻りがありません。**

---

## 7. 1.21.1 API 差分 早見表

実測で確認したもののみ記載しています。

| 項目 | 1.20.1 (`main`) | 1.21.1 |
|---|---|---|
| 食べ物判定 | `stack.getItem().isEdible()` | `stack.has(DataComponents.FOOD)` |
| `FoodData` の eat | `eat(int,float)` / `eat(Item, ItemStack[, LivingEntity])` | `eat(int,float)` / **`eat(FoodProperties)`** |
| `CakeBlock.eat` | 両者同一シグネチャ、`FoodData.eat(IF)V` を直接呼ぶ | 同左 |
| ItemStack ⇄ NBT | `stack.save(tag)` / `ItemStack.of(tag)` | `stack.save(provider)` / `ItemStack.parseOptional(provider, tag)` |
| CCA パッケージ | `dev.onyxstudios.cca` | `org.ladysnake.cca` |
| 設定 Spec | `ForgeConfigSpec` | `ModConfigSpec` (`net.neoforged.neoforge.common`) |
| データパックのディレクトリ | `data/<ns>/recipes/`, `advancements/` | `data/<ns>/recipe/`, `advancement/`（**単数形**） |
| レシピ JSON の result | `"result": {"item": "...", "count": 1}` | `"result": {"id": "...", "count": 1}` |
| Mixin 互換レベル | `JAVA_17` | `JAVA_21` |
| プラットフォーム | Fabric + Forge | Fabric + **NeoForge** |

---

## 8. 推奨作業順

1. **バスケットのスロット同期修正**（6.）— 既存の再現バグ、修正内容は確定済み
2. **ケーキ対応**（5.）— API 差分がほぼ無く、独立性が高い
3. **`basketBlacklist`**（4.）— 1 と `FoodContainer` で重なるため 1 の後
4. **中国語ローカライズ**（2.）— 単独で安全
5. **消費回数キャッシュ**（3.）— `IFoodEventHandler` を全面的に変えるため最後。
   4件の追い修正を必ずセットで
