package com.cappleapple.veiltaczlights.client;

import com.cappleapple.veiltaczlights.config.ClientConfig;
import com.cappleapple.veiltaczlights.config.FlashlightProfileRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/** Keeps all client-only setup out of the optional server datapack companion. */
public final class ClientBootstrap {
    public static void initialize(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modBus.addListener(FlashlightProfileRegistry::onConfigLoaded);
        modBus.addListener(ClientEvents::registerKeys);
    }

    private ClientBootstrap() {
    }
}
