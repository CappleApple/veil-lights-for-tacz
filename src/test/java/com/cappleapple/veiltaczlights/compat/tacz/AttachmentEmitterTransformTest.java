package com.cappleapple.veiltaczlights.compat.tacz;

import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentEmitterTransformTest {
    private static final ResourceLocation LOPRO = ResourceLocation.parse("tacz:laser_lopro");
    private static final String LAMP = "flashlight_illuminated";

    @Test
    void loproParentRotationNoLongerTurnsBeamSideways() {
        // laser_lopro_geo: flashlight_illuminated -> bone4, rotation [90, 0, 0].
        Matrix4f captured = new Matrix4f().rotateX((float) Math.PI / 2.0F);
        assertVector(new Vector3f(0, 1, 0), captured.transformDirection(new Vector3f(0, 0, -1)));
        Matrix4f corrected = AttachmentEmitterTransform.correct(LOPRO, LAMP, captured);
        assertVector(new Vector3f(0, 0, -1), corrected.transformDirection(new Vector3f(0, 0, -1)));
        assertVector(new Vector3f(0, 1, 0), corrected.transformDirection(new Vector3f(0, 1, 0)));
    }

    @Test
    void animatedGunAndMountTransformsStillControlDirection() {
        Matrix4f gun = new Matrix4f().translation(4, -2, 7)
                .rotateY(0.8F).rotateZ(-0.5F).rotateX(0.25F).scale(0.6F);
        Matrix4f captured = new Matrix4f(gun).translate(0.1F, 0.2F, -0.3F)
                .rotateX((float) Math.PI / 2.0F).translate(0.02F, 0.03F, 0.01F);
        Matrix4f corrected = AttachmentEmitterTransform.correct(LOPRO, LAMP, captured);
        assertVector(gun.transformDirection(new Vector3f(0, 0, -1)).normalize(),
                corrected.transformDirection(new Vector3f(0, 0, -1)).normalize());
        assertVector(gun.transformDirection(new Vector3f(0, 1, 0)).normalize(),
                corrected.transformDirection(new Vector3f(0, 1, 0)).normalize());
        assertVector(captured.transformPosition(new Vector3f()), corrected.transformPosition(new Vector3f()));
    }

    @Test
    void correctionNeverMutatesTheWeaponsPose() {
        Matrix4f captured = new Matrix4f().translation(1, 2, 3).rotateX((float) Math.PI / 2.0F);
        Matrix4f before = new Matrix4f(captured);
        Matrix4f corrected = AttachmentEmitterTransform.correct(LOPRO, LAMP, captured);
        assertNotSame(captured, corrected);
        assertEquals(before, captured);
    }

    @Test
    void otherAttachmentsAndExplicitAlternateBonesKeepTheirAxes() {
        Matrix4f captured = new Matrix4f().rotateY(0.7F);
        for (String id : new String[]{"tacz:laser_peq15", "tacz:laser_peq6", "tacz:laser_nightstick", "other:laser_lopro"}) {
            assertSame(captured, AttachmentEmitterTransform.correct(ResourceLocation.parse(id), LAMP, captured));
        }
        assertSame(captured, AttachmentEmitterTransform.correct(LOPRO, "@attachment", captured));
        assertSame(captured, AttachmentEmitterTransform.correct(LOPRO, "laser_beam", captured));
    }

    private static void assertVector(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x, actual.x, 1.0E-5F);
        assertEquals(expected.y, actual.y, 1.0E-5F);
        assertEquals(expected.z, actual.z, 1.0E-5F);
    }
}
