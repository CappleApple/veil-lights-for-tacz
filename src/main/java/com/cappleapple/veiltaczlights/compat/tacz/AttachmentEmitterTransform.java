package com.cappleapple.veiltaczlights.compat.tacz;

import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Model-specific emitter axes, applied only to the captured light transform. */
public final class AttachmentEmitterTransform {
    private static final ResourceLocation LOPRO = ResourceLocation.parse("tacz:laser_lopro");

    public static Matrix4f correct(ResourceLocation attachmentId, String boneName, Matrix4f captured) {
        if (LOPRO.equals(attachmentId) && "flashlight_illuminated".equals(boneName)) {
            // TaCZ's laser_lopro_geo parents the lamp to bone4, rotated +90 X.
            // Its lens faces local -Y, not the -Z assumed by other emitters.
            // Post-rotate a copy: retain the lens position and every animated
            // gun/slot transform without mutating TaCZ's live pose stack.
            return new Matrix4f(captured).rotateX(-(float) Math.PI / 2.0F);
        }
        return captured;
    }

    private AttachmentEmitterTransform() {
    }
}
