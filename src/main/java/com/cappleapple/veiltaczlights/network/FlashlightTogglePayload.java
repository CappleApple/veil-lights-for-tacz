package com.cappleapple.veiltaczlights.network;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record FlashlightTogglePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<FlashlightTogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VeilTaczLights.MOD_ID, "flashlight_toggle")
    );
    public static final StreamCodec<FriendlyByteBuf, FlashlightTogglePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    FlashlightTogglePayload::enabled,
                    FlashlightTogglePayload::new
            );

    public static void handle(FlashlightTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                FlashlightStateSynchronizer.acceptToggle(player, payload.enabled);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
