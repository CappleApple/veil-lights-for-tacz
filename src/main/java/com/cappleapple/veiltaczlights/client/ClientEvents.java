package com.cappleapple.veiltaczlights.client;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import com.cappleapple.veiltaczlights.config.FlashlightProfileRegistry;
import com.cappleapple.veiltaczlights.lighting.VeilFlashlightManager;
import com.cappleapple.veiltaczlights.lighting.FlashlightFlareRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.veiltaczlights.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.veiltaczlights"
    );

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = VeilTaczLights.MOD_ID)
    public static final class GameBus {
        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post event) {
            while (TOGGLE.consumeClick()) {
                boolean enabled = VeilFlashlightManager.toggleLocal();
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.translatable(
                            enabled ? "message.veiltaczlights.on" : "message.veiltaczlights.off"), true);
                }
            }
        }

        @SubscribeEvent
        public static void frameStart(RenderFrameEvent.Pre event) {
            VeilFlashlightManager.beginFrame();
        }

        @SubscribeEvent
        public static void frameEnd(RenderFrameEvent.Post event) {
            VeilFlashlightManager.endFrame();
        }

        @SubscribeEvent
        public static void renderLevelStage(RenderLevelStageEvent event) {
            FlashlightFlareRenderer.render(event);
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            VeilFlashlightManager.clear();
            FlashlightProfileRegistry.clearDatapackProfiles();
        }

        @SubscribeEvent
        public static void unload(LevelEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
                VeilFlashlightManager.clear();
            }
        }
    }

    private ClientEvents() {
    }
}
