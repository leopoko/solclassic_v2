package com.github.leopoko.solclassic;

import com.github.leopoko.solclassic.commands.GenerateDefaultsCommand;
import com.github.leopoko.solclassic.commands.ResetFoodHistoryCommand;
import com.github.leopoko.solclassic.container.FoodChestMenu;
import com.github.leopoko.solclassic.container.FoodContainer;
import com.github.leopoko.solclassic.item.BasketItem;
import com.github.leopoko.solclassic.item.FoodHistoryBookItem;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.ClientTooltipHandler;
import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class Solclassic {
    public static final String MOD_ID = "solclassic";

    /**
     * カスタムMenuType: クライアント・サーバー両方でFoodChestMenu（FoodSlot + FoodContainer）を使用する。
     * バニラのGENERIC_9x1を使うとクライアント側がChestMenu + SimpleContainerとなり、
     * 食べ物バリデーションの不一致によるスロット配置バグが発生する。
     */
    public static MenuType<ChestMenu> FOOD_CHEST_MENU_TYPE;

    public static void init() {
        // Write common init code here.
        // DeferredRegisterの生成（共通コード）
        final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);
        final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(MOD_ID, Registries.MENU);

        // アイテムの登録
        // WickerBasketにはFoodPropertiesを設定しない。
        // FoodPropertiesを設定するとDiet/Nutritional Balance等のMODがバスケット自体を
        // 食べ物として扱い、誤った栄養素（Carbs等）がツールチップや計算に反映される。
        // 食べるアニメーションはWickerBasketItem側のgetUseDuration/getUseAnimationで実現する。
        final RegistrySupplier<Item> WICKER_BASKET = ITEMS.register("wicker_basket", () ->
                new WickerBasketItem(new Item.Properties().stacksTo(1).arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)));

        final RegistrySupplier<Item> BASKET = ITEMS.register("basket", () ->
                new BasketItem(new Item.Properties().stacksTo(1).arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)));

        final RegistrySupplier<Item> FOOD_HISTORY_BOOK = ITEMS.register("food_history_book", () ->
                new FoodHistoryBookItem(new Item.Properties().stacksTo(1).arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)));

        // カスタムMenuTypeの登録
        // クライアント側もFoodChestMenu（FoodSlot + FoodContainer）を使用するようにする
        FOOD_CHEST_MENU_TYPE = MenuRegistry.<ChestMenu>ofExtended((syncId, inventory, buf) -> {
            int rows = buf.readInt();
            int lockedSlotIndex = buf.readInt();
            FoodContainer container = new FoodContainer(rows * 9);
            return new FoodChestMenu(Solclassic.FOOD_CHEST_MENU_TYPE, syncId, inventory, container, rows, lockedSlotIndex);
        });
        MENUS.register("food_chest", () -> FOOD_CHEST_MENU_TYPE);

        ITEMS.register();
        MENUS.register();

        // コマンドの登録
        CommandRegistrationEvent.EVENT.register((CommandDispatcher<CommandSourceStack> dispatcher,
                                                 CommandBuildContext context,
                                                 Commands.CommandSelection selection) -> {
            ResetFoodHistoryCommand.register(dispatcher);
            GenerateDefaultsCommand.register(dispatcher);
        });

        if (Platform.getEnvironment() == Env.CLIENT) {
            ClientTooltipHandler.init();
        }
    }
}
