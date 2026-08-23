package com.cappleapple.veiltaczlights.network;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import com.cappleapple.veiltaczlights.config.FlashlightProfileRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record FlashlightProfileSyncPayload(Map<ResourceLocation, String> profiles)
        implements CustomPacketPayload {
    public static final Type<FlashlightProfileSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VeilTaczLights.MOD_ID, "flashlight_profiles")
    );
    public static final StreamCodec<FriendlyByteBuf, FlashlightProfileSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.STRING_UTF8),
                    FlashlightProfileSyncPayload::profiles,
                    FlashlightProfileSyncPayload::new
            );

    public FlashlightProfileSyncPayload {
        profiles = Map.copyOf(profiles);
    }

    public static void handle(FlashlightProfileSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyClient(payload));
    }

    private static void applyClient(FlashlightProfileSyncPayload payload) {
        Map<ResourceLocation, JsonElement> parsed = new HashMap<>();
        payload.profiles.forEach((id, json) -> parsed.put(id, JsonParser.parseString(json)));
        FlashlightProfileRegistry.replaceDatapackProfiles(parsed);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
