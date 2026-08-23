package com.cappleapple.veiltaczlights.lighting;

import com.cappleapple.veilvolumelights.api.client.VeilVolumeLights;
import com.cappleapple.veilvolumelights.api.client.VolumeLight;
import com.cappleapple.veilvolumelights.api.client.VolumeLightHandle;
import org.joml.Vector3f;

import java.util.UUID;

final class FlashlightLightInstance {
    private final UUID ownerId;
    private VolumeLightHandle handle;
    private float volumetricStrength;
    int seenFrame;
    long lastDebugLog;

    FlashlightLightInstance(UUID ownerId) {
        this.ownerId = ownerId;
    }

    void update(FlashlightProfile profile, Vector3f position, Vector3f forward, Vector3f up) {
        VolumeLight.Spot definition = new VolumeLight.Spot(
                position,
                forward,
                up,
                new Vector3f(profile.red(), profile.green(), profile.blue()),
                profile.intensity(),
                profile.range(),
                profile.outerConeAngle(),
                profile.innerConeAngle(),
                profile.shadows(),
                profile.volumetricStrength()
        );
        if (handle == null) {
            handle = VeilVolumeLights.create(definition);
        } else {
            handle.update(definition);
        }
        volumetricStrength = profile.volumetricStrength();
    }

    boolean isValid() {
        return handle != null && handle.isValid();
    }

    boolean hasVolumetricBeam() {
        return volumetricStrength > 0.0F;
    }

    void remove() {
        if (handle != null) {
            handle.free();
            handle = null;
        }
        volumetricStrength = 0.0F;
    }

    UUID ownerId() {
        return ownerId;
    }

}
