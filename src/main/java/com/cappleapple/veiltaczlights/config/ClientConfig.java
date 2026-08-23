package com.cappleapple.veiltaczlights.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue RANGE;
    public static final ModConfigSpec.DoubleValue INTENSITY;
    public static final ModConfigSpec.DoubleValue OUTER_CONE_ANGLE;
    public static final ModConfigSpec.DoubleValue INNER_CONE_ANGLE;
    public static final ModConfigSpec.DoubleValue RED;
    public static final ModConfigSpec.DoubleValue GREEN;
    public static final ModConfigSpec.DoubleValue BLUE;
    public static final ModConfigSpec.BooleanValue SHADOWS;
    public static final ModConfigSpec.DoubleValue VOLUMETRIC_STRENGTH;
    public static final ModConfigSpec.BooleanValue FIRST_PERSON;
    public static final ModConfigSpec.BooleanValue THIRD_PERSON;
    public static final ModConfigSpec.DoubleValue THIRD_PERSON_DISTANCE;
    public static final ModConfigSpec.BooleanValue THIRD_PERSON_FLARE;
    public static final ModConfigSpec.DoubleValue THIRD_PERSON_FLARE_SIZE;
    public static final ModConfigSpec.DoubleValue THIRD_PERSON_FLARE_INTENSITY;
    public static final ModConfigSpec.BooleanValue DEBUG;
    public static final ModConfigSpec.BooleanValue AUTOMATIC_BONE_RECOGNITION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Veil spotlight defaults").push("light");
        ENABLED = builder.define("enabled", true);
        RANGE = builder.defineInRange("range", 32.0, 1.0, 256.0);
        INTENSITY = builder.defineInRange("intensity", 1.0, 0.0, 64.0);
        OUTER_CONE_ANGLE = builder.comment("Full outer cone angle in degrees.")
                .defineInRange("outerConeAngle", 35.0, 1.0, 179.0);
        INNER_CONE_ANGLE = builder.comment("Full inner cone angle in degrees; Veil uses the difference as its smooth falloff width.")
                .defineInRange("innerConeAngle", 20.0, 0.0, 179.0);
        RED = builder.defineInRange("red", 1.0, 0.0, 1.0);
        GREEN = builder.defineInRange("green", 0.97, 0.0, 1.0);
        BLUE = builder.defineInRange("blue", 0.90, 0.0, 1.0);
        SHADOWS = builder.comment("Uses Veil's voxel occlusion/shadowing.").define("shadows", true);
        VOLUMETRIC_STRENGTH = builder.defineInRange("volumetricStrength", 0.0, 0.0, 16.0);
        builder.pop();

        builder.comment("Rendering and recognition").push("rendering");
        FIRST_PERSON = builder.define("firstPerson", true);
        THIRD_PERSON = builder.define("thirdPerson", true);
        THIRD_PERSON_DISTANCE = builder.defineInRange("thirdPersonLightDistance", 96.0, 1.0, 512.0);
        THIRD_PERSON_FLARE = builder.comment("Render a depth-tested lens flare at active third-person flashlight emitters.")
                .define("thirdPersonFlare", true);
        THIRD_PERSON_FLARE_SIZE = builder.comment("Base flare diameter in world blocks; it grows slightly with distance to remain visible.")
                .defineInRange("thirdPersonFlareSize", 0.12, 0.01, 2.0);
        THIRD_PERSON_FLARE_INTENSITY = builder.defineInRange("thirdPersonFlareIntensity", 1.0, 0.0, 4.0);
        AUTOMATIC_BONE_RECOGNITION = builder.comment("Recognize TaCZ LASER attachments with flashlight-like model bone names.")
                .define("automaticBoneRecognition", true);
        DEBUG = builder.comment("Rate-limited transform and lifecycle logging.").define("debug", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
