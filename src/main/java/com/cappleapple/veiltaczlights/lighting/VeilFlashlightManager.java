package com.cappleapple.veiltaczlights.lighting;

import com.cappleapple.veiltaczlights.VeilTaczLights;
import com.cappleapple.veiltaczlights.config.ClientConfig;
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
import java.util.UUID;

public final class VeilFlashlightManager {
    private static final Map<UUID, FlashlightLightInstance> LIGHTS = new HashMap<>();
    private static final Map<UUID, PendingUpdate> PENDING = new HashMap<>();
    private static final Map<UUID, FlareSnapshot> FLARES = new HashMap<>();
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
        LIGHTS.values().forEach(FlashlightLightInstance::remove);
        LIGHTS.clear();
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
