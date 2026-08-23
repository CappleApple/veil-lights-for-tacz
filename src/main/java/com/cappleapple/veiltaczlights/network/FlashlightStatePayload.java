package com.cappleapple.veiltaczlights.network;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import com.cappleapple.veiltaczlights.client.ClientFlashlightNetwork;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FlashlightStatePayload(UUID playerId, boolean enabled) implements CustomPacketPayload {
    public static final Type<FlashlightStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VeilTaczLights.MOD_ID, "flashlight_state")
    );
    public static final StreamCodec<FriendlyByteBuf, FlashlightStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    FlashlightStatePayload::playerId,
                    ByteBufCodecs.BOOL,
                    FlashlightStatePayload::enabled,
                    FlashlightStatePayload::new
            );

    public static void handle(FlashlightStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientFlashlightNetwork.accept(payload));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
