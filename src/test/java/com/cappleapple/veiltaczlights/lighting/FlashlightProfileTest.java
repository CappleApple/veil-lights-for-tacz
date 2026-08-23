package com.cappleapple.veiltaczlights.lighting;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashlightProfileTest {
    @Test
    void clampsUnsafeProfileValues() {
        FlashlightProfile profile = new FlashlightProfile(
                ResourceLocation.parse("test:light"), "emitter",
                999, -2, 500, 300,
                2, -1, 0.5F, true, 99
        );

        assertEquals(256.0F, profile.range());
        assertEquals(0.0F, profile.intensity());
        assertEquals(179.0F, profile.outerConeAngle());
        assertEquals(179.0F, profile.innerConeAngle());
        assertEquals(1.0F, profile.red());
        assertEquals(0.0F, profile.green());
        assertEquals(16.0F, profile.volumetricStrength());
    }

    @Test
    void matchesOnlyConfiguredEmitterBone() {
        FlashlightProfile profile = new FlashlightProfile(
                ResourceLocation.parse("test:light"), "lamp_origin",
                32, 1, 35, 20, 1, 1, 1, true, 0
        );

        assertTrue(profile.acceptsBone("lamp_origin"));
        assertFalse(profile.acceptsBone("other"));
    }

    @Test
    void convertsBeamDiameterToFullConeAngle() {
        float widthForThirtyFiveDegrees = (float) (2.0 * 32.0 * Math.tan(Math.toRadians(17.5)));

        assertEquals(35.0F, FlashlightProfile.fullConeAngleForWidth(32.0F, widthForThirtyFiveDegrees), 0.001F);
    }
}
