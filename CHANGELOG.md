# Changelog

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
