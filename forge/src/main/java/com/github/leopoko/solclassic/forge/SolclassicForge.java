package com.github.leopoko.solclassic.forge;

import com.github.leopoko.solclassic.Solclassic;
import com.github.leopoko.solclassic.forge.config.SolClassicConfigForge;
import com.github.leopoko.solclassic.forge.integration.AppleSkinEventHandler;
import com.github.leopoko.solclassic.forge.integration.DietIntegrationForge;
import com.github.leopoko.solclassic.forge.integration.NutritionalBalanceIntegrationForge;
import com.github.leopoko.solclassic.forge.network.FoodEventHandlerForge;
import com.github.leopoko.solclassic.network.FoodHistoryHolder;
import com.github.leopoko.solclassic.network.ModNetworking;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Solclassic.MOD_ID)
public final class SolclassicForge {
    @SuppressWarnings("removal") // Forge 1.20.1ではこれらのAPIが非推奨だが、代替APIが未整備のため使用を継続
    public SolclassicForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Solclassic.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SolClassicConfigForge.SERVER_CONFIG);
        modEventBus.addListener(this::appleSkinInit);
        ModNetworking.registerPackets();

        FoodHistoryHolder.INSTANCE = new FoodEventHandlerForge();

        // Diet MOD連携: インストールされている場合のみイベントハンドラを登録
        if (ModList.get().isLoaded("diet")) {
            DietIntegrationForge.register();
        }

        // Nutritional Balance MOD連携: インストールされている場合のみイベントハンドラを登録
        if (ModList.get().isLoaded("nutritionalbalance")) {
            NutritionalBalanceIntegrationForge.register();
        }

        // Run our common setup.
        Solclassic.init();
    }

    private void appleSkinInit(final FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("appleskin")) {
            MinecraftForge.EVENT_BUS.register(new AppleSkinEventHandler());
        }
    }
}
