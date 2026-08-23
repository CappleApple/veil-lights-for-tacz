package com.cappleapple.veiltaczlights.lighting;

import net.minecraft.resources.ResourceLocation;

public record FlashlightProfile(
        ResourceLocation attachmentId,
        String emitterBone,
        float range,
        float intensity,
        float outerConeAngle,
        float innerConeAngle,
        float red,
        float green,
        float blue,
        boolean shadows,
        float volumetricStrength
) {
    public FlashlightProfile {
        range = clamp(range, 1.0F, 256.0F);
        intensity = clamp(intensity, 0.0F, 64.0F);
        outerConeAngle = clamp(outerConeAngle, 1.0F, 179.0F);
        innerConeAngle = clamp(innerConeAngle, 0.0F, outerConeAngle);
        red = clamp(red, 0.0F, 1.0F);
        green = clamp(green, 0.0F, 1.0F);
        blue = clamp(blue, 0.0F, 1.0F);
        volumetricStrength = clamp(volumetricStrength, 0.0F, 16.0F);
    }

    public boolean acceptsBone(String boneName) {
        return emitterBone == null || emitterBone.isBlank() || emitterBone.equals(boneName);
    }

    /** Converts a full beam diameter at maximum range into Veil's full cone angle. */
    public static float fullConeAngleForWidth(float length, float width) {
        float safeLength = clamp(length, 1.0F, 256.0F);
        float safeWidth = clamp(width, 0.01F, 1024.0F);
        return (float) Math.toDegrees(2.0 * Math.atan(safeWidth / (2.0F * safeLength)));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
