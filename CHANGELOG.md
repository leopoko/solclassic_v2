# Changelog

## [2.08] - 2026-03-11

### Added
- Added `enableTooltip` config option to toggle the food decay tooltip on food items. When set to false, the decay percentage tooltip will not be displayed. Default is true.
- Added `enableItemDescription` config option to toggle the description tooltip on mod items (Wicker Basket, Food History Book, etc.). When set to false, item descriptions will not be displayed. Default is true.

---

## [2.08] - 2026-03-11 (日本語)

### 追加
- `enableTooltip` 設定項目を追加。falseに設定すると、食べ物アイテムの減衰率ツールチップが非表示になる。デフォルトはtrue。
- `enableItemDescription` 設定項目を追加。falseに設定すると、MODアイテム（WickerBasket、食事記録の本等）の説明文ツールチップが非表示になる。デフォルトはtrue。

---

## [2.07] - 2026-03-10

### Added
- Added compatibility with Quality Food mod. SoL Classic's food decay now correctly applies to quality-modified nutrition values instead of base values. Food history treats items of different quality as the same food.

### Fixed
- Fixed dedicated server crash caused by FoodHistoryBookItem directly referencing client-only classes (Screen, Minecraft). Replaced client references with a Consumer<Player> callback pattern, registering the screen opener only on the client side via ClientTooltipHandler.init().

---

## [2.07] - 2026-03-10 (日本語)

### 追加
- Quality Food MODとの互換性を追加。SoL Classicの食事減衰が、品質による栄養値変更を正しく反映するよう修正。食事履歴上では品質の異なる同じ食べ物は同一として扱う。

### 修正
- FoodHistoryBookItemがクライアント専用クラス（Screen, Minecraft）を直接参照していたため、専用サーバーでクラッシュする問題を修正。クライアント参照をConsumer<Player>コールバックに置き換え、ClientTooltipHandler.init()でクライアント側のみスクリーンオープナーを登録するように変更。

---

## [2.05] - 2026-03-07

### Added
- Added global default config system for modpack creators. Place `config/solclassic-defaults.toml` to customize default values for new world server configs. Existing world configs are not affected.
- Added `/solclassic generatedefaults` command to export current server config values as a global defaults file. Use `/solclassic generatedefaults force` to overwrite an existing file.

---

## [2.05] - 2026-03-07 (日本語)

### 追加
- modpack制作者向けのグローバルデフォルトコンフィグ機能を追加。`config/solclassic-defaults.toml` を配置することで、新規ワールド作成時のサーバーコンフィグのデフォルト値をカスタマイズ可能。既存ワールドのコンフィグには影響しない。
- `/solclassic generatedefaults` コマンドを追加。現在のサーバーコンフィグ値をグローバルデフォルトファイルとして書き出す。`/solclassic generatedefaults force` で既存ファイルを上書き可能。

---

## [2.04] - 2026-03-06

### Fixed
- Fixed Wicker Basket being recognized as food by Diet/Nutritional Balance mods, causing incorrect "Carbs" tooltip and nutrient calculations. Removed FoodProperties from Wicker Basket and implemented eat animation via getUseDuration()/getUseAnimation() overrides instead.
- Fixed Wicker Basket tooltip now showing decay rate and nutritional info based on the selected food inside, instead of the basket itself.
- Fixed Diet mod tooltip appearing twice on Wicker Basket.
- Fixed IndexOutOfBoundsException when removing Diet tooltips with blank lines.
- Fixed Wicker Basket only adding Carbs when used with Nutritional Balance mod. Now correctly detects basket usage via player.getUseItem() to properly notify NB of the actual food consumed.
- Fixed race condition between Diet and Nutritional Balance event handlers where Diet's FoodDecayTracker.getAndClear() would consume decay data before NB could read it.
- Fixed Forge config not being applied to SolclassicConfigData. Now syncs on ModConfigEvent.Loading/Reloading.
- Fixed Nutritional Balance tooltip quality values not reflecting SoL Classic's decay modifier.
- Fixed Nutritional Balance tooltip color codes (§) preventing tooltip detection.

---

## [2.04] - 2026-03-06 (日本語)

### 修正
- WickerBasketがFoodPropertiesを持っていたためDiet/Nutritional Balance MODに食べ物として誤認識され、ツールチップに「Carbs」が表示される問題を修正。FoodPropertiesを除去し、食べるアニメーションはgetUseDuration()/getUseAnimation()オーバーライドで実現。
- WickerBasketのツールチップが、バスケット自体ではなく中の選択された食べ物に基づいて減衰率・栄養情報を表示するよう修正。
- WickerBasketでDietツールチップが二重表示されるバグを修正。
- Dietツールチップの空行削除時にIndexOutOfBoundsExceptionが発生する問題を修正。
- WickerBasket使用時にNutritional BalanceでCarbsしか加算されない問題を修正。player.getUseItem()でバスケット使用を検出し、実際の食べ物をNBに通知する方式に変更。
- DietとNutritional Balanceのイベントハンドラ間でFoodDecayTrackerの減衰情報が競合する問題を修正。
- Forgeの設定がSolclassicConfigDataに反映されない問題を修正。ModConfigEvent.Loading/Reloadingで自動同期するよう変更。
- Nutritional Balanceのツールチップ品質値にSoL Classicの減衰倍率が反映されない問題を修正。
- Nutritional Balanceのツールチップに含まれる色コード（§）がツールチップ検出を妨げる問題を修正。

---

## [2.03] - 2026-03-05

### Added
- Added compatibility with Diet mod.
- Added partial compatibility with Nutritional Balance.
- Added a meal record book that allows players to check their meal history.
### Fixed
- Fixed a bug where items with a stack size of 1 were not recorded when eaten.
- Fixed a bug where meal effects did not trigger when food was eaten from a Wicker Basket.

---

## [2.03] - 2026-03-05 (日本語)

### 追加
- Diet modとの互換性を追加。
- Nutritional Balanceとの部分的な互換性を追加。
- 食事履歴の確認できる食事記録の本を追加。

### 修正
- スタックが1のアイテムを食べた場合、記録されないバグを修正。
- Wicker basketで食べたときに食事の効果が発動しなかったバグの修正。

---

## [2.02] - 2026-03-05

### Added
- Added `guaranteeMinimumNutrition` config option: when set to false (default), food with 0% recovery rate will not restore any hunger. When true, guarantees a minimum of 1 hunger recovery as before.
- Added automatic publishing to CurseForge/Modrinth (mod-publish-plugin)

### Fixed
- Fixed WickerBasket not consuming food items when eating. Forge's `completeUsingItem()` copies the ItemStack before calling `finishUsingItem()`, so NBT changes in the Mixin redirect were applied to the copy instead of the original. Refactored to override `WickerBasketItem.finishUsingItem()` directly.
- Fixed Fabric crash caused by common Mixin config referencing Forge-only class `DisableConflictingMixinPlugin`. Removed from `solclassic.mixins.json` as it is already referenced in `solclassic-forge.mixins.json`.

---

## [2.02] - 2026-03-05 (日本語)

### 追加
- `guaranteeMinimumNutrition` 設定項目を追加。false（デフォルト）の場合、減衰で回復率が0%になった食べ物は満腹度を回復しない。trueの場合は従来通り最低1の回復を保証する。
- CurseForge/Modrinthへの自動公開機能を追加 (mod-publish-plugin)

### 修正
- WickerBasketで食事時にバスケット内アイテムが消費されないバグを修正。ForgeのcompleteUsingItem()がItemStackをコピーしてからfinishUsingItem()を呼ぶため、Mixinのリダイレクト内でのNBT変更がコピーに対して行われ元のアイテムに反映されなかった。WickerBasketItem.finishUsingItem()を直接オーバーライドする方式に変更。
- 共通Mixin設定(solclassic.mixins.json)がForge専用クラスDisableConflictingMixinPluginを参照していたため、Fabric起動時にクラッシュするバグを修正。solclassic-forge.mixins.jsonで既に参照されているため、共通設定から削除。
