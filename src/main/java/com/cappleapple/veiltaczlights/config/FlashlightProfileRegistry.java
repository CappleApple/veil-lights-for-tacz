package com.cappleapple.veiltaczlights.config;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import com.cappleapple.veiltaczlights.lighting.FlashlightProfile;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class FlashlightProfileRegistry {
    private static final String FILE_NAME = "veiltaczlights-attachments.toml";
    private static volatile Map<ResourceLocation, Entry> tomlEntries = Map.of();
    private static volatile Map<ResourceLocation, Entry> datapackEntries = Map.of();

    public static void onConfigLoaded(ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.CLIENT
                && event.getConfig().getModId().equals(VeilTaczLights.MOD_ID)) {
            reload();
        }
    }

    public static synchronized void reload() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        installDefault(path);
        Map<ResourceLocation, Entry> loaded = new HashMap<>();
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            Config table = config.get("attachments");
            if (table != null) {
                for (Config.Entry rawEntry : table.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(rawEntry.getKey());
                    if (id == null || !(rawEntry.getValue() instanceof Config profileConfig)) {
                        VeilTaczLights.LOGGER.warn("Ignoring invalid flashlight profile key: {}", rawEntry.getKey());
                        continue;
                    }
                    boolean enabled = bool(profileConfig, "enabled", true);
                    String emitterBone = string(profileConfig, "emitterBone", "flashlight");
                    loaded.put(id, new Entry(enabled, resolve(id, emitterBone, profileConfig)));
                }
            }
            tomlEntries = Map.copyOf(loaded);
            VeilTaczLights.LOGGER.info("Loaded {} legacy TOML TaCZ flashlight attachment profiles", tomlEntries.size());
        } catch (RuntimeException exception) {
            VeilTaczLights.LOGGER.error("Failed to load {}. Keeping the previous profiles.", path, exception);
        }
    }

    public static Optional<FlashlightProfile> explicit(ResourceLocation id) {
        Entry entry = entry(id);
        return entry == null || !entry.enabled ? Optional.empty() : Optional.of(entry.profile);
    }

    public static boolean explicitlyDisabled(ResourceLocation id) {
        Entry entry = entry(id);
        return entry != null && !entry.enabled;
    }

    public static FlashlightProfile automatic(ResourceLocation id, String emitterBone) {
        return resolve(id, emitterBone, null);
    }

    public static synchronized void replaceDatapackProfiles(Map<ResourceLocation, JsonElement> resources) {
        Map<ResourceLocation, Entry> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> resource : resources.entrySet()) {
            try {
                loaded.put(resource.getKey(), parseDatapackProfile(resource.getKey(), resource.getValue()));
            } catch (IllegalArgumentException | JsonParseException exception) {
                VeilTaczLights.LOGGER.error("Ignoring invalid flashlight datapack profile {}", resource.getKey(), exception);
            }
        }
        datapackEntries = Map.copyOf(loaded);
        VeilTaczLights.LOGGER.info("Loaded {} datapack TaCZ flashlight attachment profiles", datapackEntries.size());
    }

    public static synchronized void clearDatapackProfiles() {
        datapackEntries = Map.of();
    }

    private static Entry entry(ResourceLocation id) {
        Entry datapack = datapackEntries.get(id);
        return datapack != null ? datapack : tomlEntries.get(id);
    }

    private static Entry parseDatapackProfile(ResourceLocation id, JsonElement element) {
        JsonObject root = GsonHelper.convertToJsonObject(element, "flashlight profile");
        boolean enabled = GsonHelper.getAsBoolean(root, "enabled", true);
        String emitterBone = GsonHelper.getAsString(root, "emitter_bone", "flashlight");
        JsonObject beam = root.has("beam") ? GsonHelper.getAsJsonObject(root, "beam") : root;

        FlashlightProfile fallback = resolve(id, emitterBone, null);
        float length = clamp(number(beam, "length", fallback.range()), 1.0F, 256.0F);

        float outerAngle = fallback.outerConeAngle();
        boolean hasWidth = beam.has("width");
        float width = hasWidth ? positiveNumber(beam, "width") : 0.0F;
        if (hasWidth) {
            outerAngle = FlashlightProfile.fullConeAngleForWidth(length, width);
        }

        float innerAngle = fallback.innerConeAngle();
        if (beam.has("inner_width")) {
            innerAngle = FlashlightProfile.fullConeAngleForWidth(length, positiveNumber(beam, "inner_width"));
        } else if (hasWidth) {
            float outerRadius = (float) Math.tan(Math.toRadians(fallback.outerConeAngle() * 0.5F));
            float innerRadius = (float) Math.tan(Math.toRadians(fallback.innerConeAngle() * 0.5F));
            float innerRatio = outerRadius <= 1.0E-6F ? 0.55F : innerRadius / outerRadius;
            innerAngle = FlashlightProfile.fullConeAngleForWidth(length, width * innerRatio);
        }

        float[] color = color(beam.get("color"), fallback.red(), fallback.green(), fallback.blue());
        FlashlightProfile profile = new FlashlightProfile(
                id,
                emitterBone,
                length,
                number(beam, "intensity", fallback.intensity()),
                outerAngle,
                innerAngle,
                color[0],
                color[1],
                color[2],
                bool(beam, "shadows", fallback.shadows()),
                number(beam, "volumetric_strength", fallback.volumetricStrength())
        );
        return new Entry(enabled, profile);
    }

    private static float number(JsonObject object, String key, float fallback) {
        return object.has(key) ? GsonHelper.convertToFloat(object.get(key), key) : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? GsonHelper.convertToBoolean(object.get(key), key) : fallback;
    }

    private static float positiveNumber(JsonObject object, String key) {
        float value = GsonHelper.convertToFloat(object.get(key), key);
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new JsonParseException(key + " must be a positive finite number");
        }
        return value;
    }

    private static float[] color(JsonElement element, float fallbackRed, float fallbackGreen, float fallbackBlue) {
        if (element == null || element.isJsonNull()) {
            return new float[]{fallbackRed, fallbackGreen, fallbackBlue};
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            if (value.length() != 6) {
                throw new JsonParseException("color must use #RRGGBB");
            }
            try {
                int rgb = Integer.parseUnsignedInt(value, 16);
                return new float[]{
                        ((rgb >> 16) & 0xFF) / 255.0F,
                        ((rgb >> 8) & 0xFF) / 255.0F,
                        (rgb & 0xFF) / 255.0F
                };
            } catch (NumberFormatException exception) {
                throw new JsonParseException("color must use #RRGGBB", exception);
            }
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() != 3) {
                throw new JsonParseException("color array must contain exactly three components");
            }
            return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
        }
        throw new JsonParseException("color must be #RRGGBB or a three-number RGB array");
    }

    private static float clamp(float value, float minimum, float maximum) {
        if (!Float.isFinite(value)) {
            throw new JsonParseException("beam value must be finite");
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static FlashlightProfile resolve(ResourceLocation id, String emitterBone, Config config) {
        return new FlashlightProfile(
                id,
                emitterBone,
                number(config, "range", ClientConfig.RANGE.get()).floatValue(),
                number(config, "intensity", ClientConfig.INTENSITY.get()).floatValue(),
                number(config, "outerConeAngle", ClientConfig.OUTER_CONE_ANGLE.get()).floatValue(),
                number(config, "innerConeAngle", ClientConfig.INNER_CONE_ANGLE.get()).floatValue(),
                number(config, "red", ClientConfig.RED.get()).floatValue(),
                number(config, "green", ClientConfig.GREEN.get()).floatValue(),
                number(config, "blue", ClientConfig.BLUE.get()).floatValue(),
                bool(config, "shadows", ClientConfig.SHADOWS.get()),
                number(config, "volumetricStrength", ClientConfig.VOLUMETRIC_STRENGTH.get()).floatValue()
        );
    }

    private static Number number(Config config, String key, Number fallback) {
        return config == null ? fallback : config.getOrElse(key, fallback);
    }

    private static boolean bool(Config config, String key, boolean fallback) {
        return config == null ? fallback : config.getOrElse(key, fallback);
    }

    private static String string(Config config, String key, String fallback) {
        return config == null ? fallback : config.getOrElse(key, fallback);
    }

    private static void installDefault(Path path) {
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            try (InputStream stream = FlashlightProfileRegistry.class.getResourceAsStream("/default-veiltaczlights-attachments.toml")) {
                if (stream != null) {
                    Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException exception) {
            VeilTaczLights.LOGGER.error("Could not create default flashlight profile file {}", path, exception);
        }
    }

    private record Entry(boolean enabled, FlashlightProfile profile) {
    }

    private FlashlightProfileRegistry() {
    }
}
