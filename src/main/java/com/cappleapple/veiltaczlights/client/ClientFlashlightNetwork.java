package com.cappleapple.veiltaczlights.client;

import com.cappleapple.veiltaczlights.lighting.VeilFlashlightManager;
import com.cappleapple.veiltaczlights.network.FlashlightStatePayload;
import com.cappleapple.veiltaczlights.network.FlashlightTogglePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientFlashlightNetwork {
    public static void sendState(boolean enabled) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener != null && listener.hasChannel(FlashlightTogglePayload.TYPE)) {
            PacketDistributor.sendToServer(new FlashlightTogglePayload(enabled));
        }
    }

    public static void accept(FlashlightStatePayload payload) {
        VeilFlashlightManager.applySynchronizedState(payload.playerId(), payload.enabled());
    }

    private ClientFlashlightNetwork() {
    }
}
