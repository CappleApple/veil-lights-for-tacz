package com.cappleapple.veiltaczlights.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FlashlightStateSynchronizer {
    private static final PlayerFlashlightStateStore STATES = new PlayerFlashlightStateStore();

    public static void acceptToggle(ServerPlayer player, boolean enabled) {
        STATES.set(player.getUUID(), enabled);
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        FlashlightStatePayload payload = new FlashlightStatePayload(player.getUUID(), enabled);
        server.getPlayerList().getPlayers().stream()
                .filter(observer -> observer.connection.hasChannel(FlashlightStatePayload.TYPE))
                .forEach(observer -> PacketDistributor.sendToPlayer(observer, payload));
    }

    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.reset(player.getUUID());
            sendState(player, player);
        }
    }

    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.reset(player.getUUID());
        }
    }

    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer
                && event.getTarget() instanceof ServerPlayer target) {
            sendState(observer, target);
        }
    }

    public static void serverStopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    private static void sendState(ServerPlayer observer, ServerPlayer subject) {
        if (observer.connection.hasChannel(FlashlightStatePayload.TYPE)) {
            PacketDistributor.sendToPlayer(observer, new FlashlightStatePayload(
                    subject.getUUID(), STATES.isEnabled(subject.getUUID())
            ));
        }
    }

    private FlashlightStateSynchronizer() {
    }
}
