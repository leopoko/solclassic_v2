package com.github.leopoko.solclassic;

import com.github.leopoko.solclassic.commands.ResetFoodHistoryCommand;
import com.github.leopoko.solclassic.item.BasketItem;
import com.github.leopoko.solclassic.item.WickerBasketItem;
import com.github.leopoko.solclassic.utils.ClientTooltipHandler;
import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class Solclassic {
    public static final String MOD_ID = "solclassic";

    public static void init() {
        // Write common init code here.
        // DeferredRegisterの生成（共通コード）
        final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);

        // アイテムの登録
        final RegistrySupplier<Item> WICKER_BASKET = ITEMS.register("wicker_basket", () ->
                new WickerBasketItem(new Item.Properties().stacksTo(1).food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1f).build()).arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)));

        final RegistrySupplier<Item> BASKET = ITEMS.register("basket", () ->
                new BasketItem(new Item.Properties().stacksTo(1).arch$tab(CreativeModeTabs.TOOLS_AND_UTILITIES)));

        ITEMS.register();

        // コマンドの登録
        CommandRegistrationEvent.EVENT.register((CommandDispatcher<CommandSourceStack> dispatcher,
                                                 CommandBuildContext context,
                                                 Commands.CommandSelection selection) -> {
            ResetFoodHistoryCommand.register(dispatcher);
        });

        if (Platform.getEnvironment() == Env.CLIENT) {
            ClientTooltipHandler.init();
        }
    }
}
