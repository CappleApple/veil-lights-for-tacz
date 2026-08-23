package com.cappleapple.veiltaczlights.config;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import com.cappleapple.veiltaczlights.network.FlashlightProfileSyncPayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/** Loads data/&lt;namespace&gt;/veiltaczlights/attachment_profiles/*.json. */
public final class DatapackFlashlightProfileLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIRECTORY = "veiltaczlights/attachment_profiles";
    private static volatile Map<ResourceLocation, String> profiles = Map.of();

    private DatapackFlashlightProfileLoader() {
        super(GSON, DIRECTORY);
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new DatapackFlashlightProfileLoader());
    }

    public static void sync(OnDatapackSyncEvent event) {
        FlashlightProfileSyncPayload payload = new FlashlightProfileSyncPayload(profiles);
        event.getRelevantPlayers()
                .filter(player -> player.connection.hasChannel(FlashlightProfileSyncPayload.TYPE))
                .forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, String> serialized = new java.util.HashMap<>();
        resources.forEach((id, json) -> serialized.put(id, json.toString()));
        profiles = Map.copyOf(serialized);
        VeilTaczLights.LOGGER.info("Prepared {} datapack TaCZ flashlight attachment profiles for client sync", profiles.size());
    }
}
