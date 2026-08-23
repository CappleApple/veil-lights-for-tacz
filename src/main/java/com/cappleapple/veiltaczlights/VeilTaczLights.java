package com.cappleapple.veiltaczlights;

import com.cappleapple.veiltaczlights.client.ClientBootstrap;
import com.cappleapple.veiltaczlights.config.DatapackFlashlightProfileLoader;
import com.cappleapple.veiltaczlights.content.ModContent;
import com.cappleapple.veiltaczlights.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(VeilTaczLights.MOD_ID)
public final class VeilTaczLights {
    public static final String MOD_ID = "veiltaczlights";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VeilTaczLights(IEventBus modBus, ModContainer container) {
        ModContent.register(modBus);
        modBus.addListener(NetworkHandler::register);
        NeoForge.EVENT_BUS.addListener(DatapackFlashlightProfileLoader::register);
        NeoForge.EVENT_BUS.addListener(DatapackFlashlightProfileLoader::sync);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.initialize(modBus, container);
        }
    }
}
