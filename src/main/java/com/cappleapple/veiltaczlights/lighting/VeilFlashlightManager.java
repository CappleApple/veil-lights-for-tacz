package com.cappleapple.veiltaczlights.lighting;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import com.cappleapple.veiltaczlights.config.ClientConfig;
import com.cappleapple.veiltaczlights.content.ArtificialBeamSourceTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class VeilFlashlightManager {
    private static final Map<UUID, FlashlightLightInstance> LIGHTS = new HashMap<>();
    private static final Map<UUID, PendingUpdate> PENDING = new HashMap<>();
    private static final Map<UUID, FlareSnapshot> FLARES = new HashMap<>();
    private static final Map<Long, ArtificialBeamSourceTracker.Source> ARTIFICIAL_SOURCES = new HashMap<>();
    private static final Map<Long, FlashlightLightInstance> ARTIFICIAL_LIGHTS = new HashMap<>();
    private static final FlashlightProfile ARTIFICIAL_PROFILE = new FlashlightProfile(
            ResourceLocation.fromNamespaceAndPath(VeilTaczLights.MOD_ID, "artificial_beam_source"),
            "artificial_beam_source",
            40.0F,
            1.15F,
            FlashlightProfile.fullConeAngleForWidth(40.0F, 14.0F),
            FlashlightProfile.fullConeAngleForWidth(40.0F, 6.5F),
            0xF2 / 255.0F,
            0xFA / 255.0F,
            1.0F,
            true,
            0.85F
    );
    private static int frame;
    private static boolean localEnabled = true;

    public static void beginFrame() {
        frame++;
        PENDING.clear();
        FLARES.clear();
        if (frame == Integer.MAX_VALUE) {
            frame = 1;
            LIGHTS.values().forEach(instance -> instance.seenFrame = 0);
        }
    }

    public static void capture(LivingEntity owner, ResourceLocation gunId, ResourceLocation attachmentId,
                               boolean firstPerson, FlashlightProfile profile, Matrix4f renderedTransform,
                               String boneName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientConfig.ENABLED.get() || minecraft.level == null || !owner.isAlive()) {
            return;
        }
        if (firstPerson && !ClientConfig.FIRST_PERSON.get()) {
            return;
        }
        if (!firstPerson && !ClientConfig.THIRD_PERSON.get()) {
            return;
        }
        if (minecraft.player != null && owner.getUUID().equals(minecraft.player.getUUID()) && !localEnabled) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vector3f cameraRelative = renderedTransform.transformPosition(new Vector3f());
        Vector3f position = cameraRelative.add(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z
        );

        if (!firstPerson) {
            double maxDistance = ClientConfig.THIRD_PERSON_DISTANCE.get();
            if (position.distanceSquared(
                    (float) camera.getPosition().x,
                    (float) camera.getPosition().y,
                    (float) camera.getPosition().z) > maxDistance * maxDistance) {
                return;
            }
        }

        Vector3f forward = renderedTransform.transformDirection(new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f up = renderedTransform.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F));
        if (forward.lengthSquared() < 1.0E-6F || up.lengthSquared() < 1.0E-6F) {
            return;
        }
        forward.normalize();
        up.normalize();

        if (!firstPerson && ClientConfig.THIRD_PERSON_FLARE.get()) {
            FLARES.put(owner.getUUID(), new FlareSnapshot(
                    new Vector3f(position), new Vector3f(forward), profile
            ));
        }

        // Never create or mutate Veil render resources from inside TaCZ's model
        // traversal. Creating the first renderer/VAO there can invalidate the
        // bindings TaCZ is actively using and make the weapon or living model
        // disappear. The queued update is applied from RenderFrameEvent.Post.
        PENDING.put(owner.getUUID(), new PendingUpdate(
                owner.getUUID(), gunId, attachmentId, profile,
                new Vector3f(position), new Vector3f(forward), new Vector3f(up), boneName
        ));
    }

    public static void endFrame() {
        for (PendingUpdate update : PENDING.values()) {
            FlashlightLightInstance instance = LIGHTS.computeIfAbsent(update.ownerId, FlashlightLightInstance::new);
            instance.seenFrame = frame;
            instance.update(update.profile, update.position, update.forward, update.up);
            debug(instance, update.gunId, update.attachmentId, update.boneName, update.position, update.forward);
        }
        PENDING.clear();

        updateArtificialLights();

        Iterator<FlashlightLightInstance> iterator = LIGHTS.values().iterator();
        while (iterator.hasNext()) {
            FlashlightLightInstance instance = iterator.next();
            if (instance.seenFrame != frame || !instance.isValid()) {
                instance.remove();
                iterator.remove();
            }
        }

    }

    public static boolean toggleLocal() {
        localEnabled = !localEnabled;
        if (!localEnabled) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                PENDING.remove(minecraft.player.getUUID());
                FLARES.remove(minecraft.player.getUUID());
                remove(minecraft.player.getUUID());
            }
        }
        return localEnabled;
    }

    public static void clear() {
        PENDING.clear();
        FLARES.clear();
        ARTIFICIAL_SOURCES.clear();
        LIGHTS.values().forEach(FlashlightLightInstance::remove);
        LIGHTS.clear();
        ARTIFICIAL_LIGHTS.values().forEach(FlashlightLightInstance::remove);
        ARTIFICIAL_LIGHTS.clear();
    }

    private static void remove(UUID ownerId) {
        FlashlightLightInstance instance = LIGHTS.remove(ownerId);
        if (instance != null) {
            instance.remove();
        }
    }

    public static Collection<FlareSnapshot> activeThirdPersonFlares() {
        return Collections.unmodifiableCollection(FLARES.values());
    }

    public static void syncArtificialSources(Collection<ArtificialBeamSourceTracker.Source> sources) {
        ARTIFICIAL_SOURCES.clear();
        sources.forEach(source -> ARTIFICIAL_SOURCES.put(source.pos().asLong(), source));
    }

    private static void updateArtificialLights() {
        if (!ClientConfig.ENABLED.get()) {
            ARTIFICIAL_LIGHTS.values().forEach(FlashlightLightInstance::remove);
            ARTIFICIAL_LIGHTS.clear();
            return;
        }

        for (Map.Entry<Long, ArtificialBeamSourceTracker.Source> entry : ARTIFICIAL_SOURCES.entrySet()) {
            long key = entry.getKey();
            ArtificialBeamSourceTracker.Source source = entry.getValue();
            Vector3f forward = new Vector3f(
                    source.facing().getStepX(),
                    source.facing().getStepY(),
                    source.facing().getStepZ()
            );
            Vector3f position = new Vector3f(
                    source.pos().getX() + 0.5F,
                    source.pos().getY() + 0.5F,
                    source.pos().getZ() + 0.5F
            ).fma(0.52F, forward);
            FlashlightLightInstance instance = ARTIFICIAL_LIGHTS.computeIfAbsent(
                    key,
                    ignored -> new FlashlightLightInstance(new UUID(0x4152544946494349L, key))
            );
            instance.update(ARTIFICIAL_PROFILE, position, forward, new Vector3f(0.0F, 1.0F, 0.0F));
        }

        Set<Long> activeKeys = ARTIFICIAL_SOURCES.keySet().stream().collect(Collectors.toSet());
        ARTIFICIAL_LIGHTS.entrySet().removeIf(entry -> {
            if (activeKeys.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().remove();
            return true;
        });
    }

    public record FlareSnapshot(Vector3f position, Vector3f forward, FlashlightProfile profile) {
    }

    private static void debug(FlashlightLightInstance instance, ResourceLocation gunId,
                              ResourceLocation attachmentId, String boneName,
                              Vector3f position, Vector3f forward) {
        if (!ClientConfig.DEBUG.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - instance.lastDebugLog < 1_000L) {
            return;
        }
        instance.lastDebugLog = now;
        VeilTaczLights.LOGGER.info(
                "TaCZ flashlight owner={} gun={} attachment={} bone={} enabled=true pos=[{},{},{}] forward=[{},{},{}] veilLight={}",
                instance.ownerId(), gunId, attachmentId, boneName,
                position.x, position.y, position.z, forward.x, forward.y, forward.z, instance.isValid()
        );
    }

    private record PendingUpdate(
            UUID ownerId,
            ResourceLocation gunId,
            ResourceLocation attachmentId,
            FlashlightProfile profile,
            Vector3f position,
            Vector3f forward,
            Vector3f up,
            String boneName
    ) {
    }

    private VeilFlashlightManager() {
    }
}
