package com.cappleapple.veiltaczlights.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(
                FlashlightProfileSyncPayload.TYPE,
                FlashlightProfileSyncPayload.STREAM_CODEC,
                FlashlightProfileSyncPayload::handle
        );
        registrar.playToServer(
                FlashlightTogglePayload.TYPE,
                FlashlightTogglePayload.STREAM_CODEC,
                FlashlightTogglePayload::handle
        );
        registrar.playToClient(
                FlashlightStatePayload.TYPE,
                FlashlightStatePayload.STREAM_CODEC,
                FlashlightStatePayload::handle
        );
    }

    private NetworkHandler() {
    }
}
