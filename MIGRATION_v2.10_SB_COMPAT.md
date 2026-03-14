# v2.10 Sophisticated Backpacks互換性 - 1.21.1移行ガイド

## 概要

v2.10で追加されたSophisticated Backpacks (SB) のFeeding Upgrade互換機能を1.21.1ブランチに移行するためのガイド。

## 変更されたファイル一覧 (1.20.1/Forge)

### 新規ファイル
| ファイル | 役割 |
|---------|------|
| `forge/src/main/java/.../mixin/SBFeedingUpgradeMixin.java` | SBのFeedingUpgradeWrapperに直接介入するMixin |
| `forge/src/main/java/.../mixin/SBCompatMixinPlugin.java` | SB存在時のみMixinを動的登録するプラグイン |
| `forge/src/main/resources/solclassic-sb-compat.mixins.json` | SB互換Mixin用の設定ファイル |

### 変更ファイル
| ファイル | 変更内容 |
|---------|---------|
| `forge/build.gradle` | SophisticatedCore/Backpacksの`modCompileOnlyApi`依存追加、`mixinConfig`追加 |
| `forge/src/main/resources/solclassic-forge.mixins.json` | SBFeedingUpgradeMixinを削除（sb-compat側に移動） |
| `common/src/main/java/.../item/WickerBasketItem.java` | `shrinkItemFromInventory`を`public`に変更 |
| `forge/src/main/java/.../mixin/WickerBasketMixinForge.java` | SBバックパック内ではgetFoodProperties=nullを返す |
| `forge/src/main/java/.../mixin/PlayerMixinForge.java` | WickerBasketが直接eat()に渡された場合の処理追加 |

---

## 1.21.1での移行手順

### 1. SBFeedingUpgradeMixin.java の移行

**配置先**: `neoforge/src/main/java/com/github/leopoko/solclassic/neoforge/mixin/SBFeedingUpgradeMixin.java`

**注意点**:
- パッケージを `forge.mixin` → `neoforge.mixin` に変更
- SophisticatedCoreの1.21.1版でFeedingUpgradeWrapperのメソッドシグネチャを確認すること
  - `isEdible(ItemStack, LivingEntity)` が変更されている可能性あり
  - `tryFeedingStack(Level, int, Player, Integer, ItemStack, ITrackedContentsItemHandler)` の引数が変更されている可能性あり
- `BuiltInRegistries.ITEM` は1.21.1でも使用可能だが、`Registries.ITEM`に変わっている可能性を確認
- `FoodProperties`の取得方法が異なる:
  ```java
  // 1.20.1
  FoodProperties fp = stack.getItem().getFoodProperties();
  // 1.21.1
  FoodProperties fp = stack.get(DataComponents.FOOD);
  ```
- `remap = false` の指定はNeoForge環境でも必要（SBのメソッドはMojang mappingsではないため）

**isEdible メソッドのポイント**:
```java
// 1.20.1 (現在の実装)
@Inject(method = "isEdible", at = @At("RETURN"), cancellable = true, require = 0)
private static void solclassic$skipZeroDecayFood(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
    // ...
    FoodProperties fp = stack.getItem().getFoodProperties();  // ← 1.21.1では変更必要
    // ...
}
```

**tryFeedingStack メソッドのポイント**:
```java
// 1.20.1 (現在の実装)
@Inject(method = "tryFeedingStack", at = @At("HEAD"), cancellable = true, require = 0)
private void solclassic$handleWickerBasketFeeding(Level level, int hungerLevel, Player player,
    Integer slot, ItemStack stack, ITrackedContentsItemHandler inventory,
    CallbackInfoReturnable<Boolean> cir) {
    // ...
    player.eat(level, foodCopy);  // ← 1.21.1では player.eat(level, foodCopy, foodProperties) に変更
    // ...
}
```

### 2. SBCompatMixinPlugin.java の移行

**配置先**: `neoforge/src/main/java/com/github/leopoko/solclassic/neoforge/mixin/SBCompatMixinPlugin.java`

**注意点**:
- `IMixinConfigPlugin` のインターフェースはMixin APのものなので変更なし
- クラスパスチェックのパスが変更されている可能性:
  ```java
  // 1.20.1
  private static final String SB_FEEDING_CLASS = "net/p3pp3rf1y/sophisticatedcore/upgrades/feeding/FeedingUpgradeWrapper.class";
  // 1.21.1 - SophisticatedCoreのパッケージ構造を確認すること
  ```
- 1.21.1ではNeoForgeの`IMixinConfigPlugin`のロード順が変わっている可能性あり。テストで確認

### 3. solclassic-sb-compat.mixins.json の移行

**配置先**: `neoforge/src/main/resources/solclassic-sb-compat.mixins.json`

```json
{
  "required": false,
  "package": "com.github.leopoko.solclassic.neoforge.mixin",  // ← neoforgeに変更
  "compatibilityLevel": "JAVA_21",  // ← 17から21に変更
  "plugin": "com.github.leopoko.solclassic.neoforge.mixin.SBCompatMixinPlugin",  // ← neoforgeに変更
  "mixins": [],
  "injectors": {
    "defaultRequire": 0
  }
}
```

### 4. neoforge/build.gradle の変更

```groovy
// SophisticatedCore/Backpacks の1.21.1版をCurseForge Mavenから追加
// ※ FileIDはCurseForgeで1.21.1版を確認して更新すること
modCompileOnlyApi "curse.maven:sophisticatedcore-618298:XXXXX"
modCompileOnlyApi "curse.maven:sophisticatedbackpacks-422301:XXXXX"

// Mixinコンフィグ追加
loom {
    neoForge {  // ← forgeではなくneoForge
        mixinConfig "solclassic-sb-compat.mixins.json"
    }
}
```

### 5. WickerBasketItem.java の変更

`shrinkItemFromInventory` が `public` であることを確認。1.21.1ブランチで`private`の場合は`public`に変更。

**1.21.1固有の注意点**:
- NBTデータの操作方法が異なる（`DataComponents.CUSTOM_DATA` 経由）
- `shrinkItemFromInventory`内のNBT操作コードを確認し、1.21.1のAPIに合わせること

### 6. WickerBasketMixin の移行

1.20.1の`WickerBasketMixinForge`は`getFoodProperties(ItemStack, LivingEntity)`をオーバーライドしている。

**1.21.1での対応**:
- NeoForge 1.21.1でも`getFoodProperties(ItemStack, LivingEntity)`がForgeパッチとして存在するか確認
- 存在しない場合、DataComponents経由で`FoodProperties`を制御する別の方法を検討
- WickerBasketが食べ物として認識されないようにする機構が必要

### 7. PlayerMixin の変更

1.20.1で追加された`wickerBasketStack`変数と関連ロジックの移行。

**1.21.1との差異**:
```java
// 1.20.1: Player.eat(Level, ItemStack) → FoodData.eat(Item, ItemStack, LivingEntity)
// 1.21.1: Player.eat(Level, ItemStack, FoodProperties) → FoodData.eat(int, float)  ※要確認
```
- `Player.eat()`のシグネチャが`(Level, ItemStack, FoodProperties)`に変更
- FoodDataのAPIも変更されている可能性がある
- WickerBasketが直接`eat()`に渡された場合の`shrinkItemFromInventory`呼び出しロジックは同様に必要

---

## SophisticatedCore 1.21.1のAPI確認チェックリスト

移行前に以下を確認すること:

- [ ] `FeedingUpgradeWrapper`クラスのパッケージパスが同じか
- [ ] `isEdible(ItemStack, LivingEntity)` のシグネチャが同じか
- [ ] `tryFeedingStack(...)` のシグネチャと引数順序が同じか
- [ ] `ITrackedContentsItemHandler`のパッケージパスが同じか
- [ ] SophisticatedCore 1.21.1版のCurseForge FileIDを確認
- [ ] SophisticatedBackpacks 1.21.1版のCurseForge FileIDを確認

## テスト手順

1. **SBなしでの起動テスト**: クラッシュしないことを確認
2. **SBありでの動作テスト**:
   - 減衰率0%の食べ物がFeeding Upgradeで自動消費されないこと
   - WickerBasketがSBバックパック内にある場合、バスケット内の食べ物が優先的に食べられること
   - WickerBasketがプレイヤーインベントリにある場合、AppleSkinのツールチップが正常に表示されること
   - バスケット内の食べ物が消費後にNBTから正しく削除されること

## 修正の背景

### 症状
- SBのFeeding UpgradeがSoL Classicの食事減衰を無視し、回復率0%の食べ物も自動消費していた
- WickerBasketがSBバックパックに入っていても、中の食べ物ではなく個別の食べ物アイテムが消費されていた

### 原因
SBのFeedingUpgradeWrapperは独自の食べ物判定ロジック（`isEdible`）を使用し、Forgeの`getFoodProperties(ItemStack, LivingEntity)`を参照するが、SoL Classicのプレイヤー固有の減衰情報は考慮されていなかった。

### 解決方法
- `Item.getFoodProperties`をグローバルに変更する方法は影響範囲が大きいため却下
- SBのFeedingUpgradeWrapperに直接Mixinで介入し、`isEdible`で0%減衰チェック、`tryFeedingStack`でWickerBasket専用の食事処理を実装

### 初回実装時のバグと修正
1. **メソッドシグネチャ不一致**: `isEdible`の第2引数が`LivingEntity`（実際）なのに`Player`で受けていた。また`tryFeedingStack`の第6引数`ITrackedContentsItemHandler`が欠けていた。→ Mixinが適用されなかった
2. **SB未インストール時のクラッシュ**: `solclassic-forge.mixins.json`にSBFeedingUpgradeMixinを直接記述するとターゲットクラス不在でクラッシュ。→ 別設定ファイル + `IMixinConfigPlugin.getMixins()`で条件付きロードに変更
