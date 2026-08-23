package com.cappleapple.veiltaczlights.lighting;

import com.cappleapple.veiltaczlights.config.ClientConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Draws a small world-space glow at visible third-person flashlight lenses. */
public final class FlashlightFlareRenderer {
    private static final ResourceLocation FLARE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/particle/glow.png");

    // Source alpha controls how much light is added, preserving the soft alpha
    // edge of Minecraft's glow texture without darkening the scene behind it.
    private static final RenderStateShard.TransparencyStateShard ALPHA_ADDITIVE =
            new RenderStateShard.TransparencyStateShard(
                    "veiltaczlights_alpha_additive",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE
                        );
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );

    private static final RenderType RENDER_TYPE = RenderType.create(
            "veiltaczlights_flashlight_flare",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(FLARE_TEXTURE, false, false))
                    .setTransparencyState(ALPHA_ADDITIVE)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(false)
    );

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !ClientConfig.THIRD_PERSON_FLARE.get()
                || VeilFlashlightManager.activeThirdPersonFlares().isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        Quaternionf billboardRotation = new Quaternionf(camera.rotation());
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);

        for (VeilFlashlightManager.FlareSnapshot flare : VeilFlashlightManager.activeThirdPersonFlares()) {
            drawFlare(consumer, billboardRotation, cameraPosition, flare);
        }

        bufferSource.endBatch(RENDER_TYPE);
    }

    private static void drawFlare(VertexConsumer consumer, Quaternionf billboardRotation, Vec3 cameraPosition,
                                  VeilFlashlightManager.FlareSnapshot flare) {
        Vector3f position = new Vector3f(flare.position()).fma(0.025F, flare.forward());
        Vector3f toCamera = new Vector3f(
                (float) cameraPosition.x - position.x,
                (float) cameraPosition.y - position.y,
                (float) cameraPosition.z - position.z
        );
        float distanceSquared = toCamera.lengthSquared();
        if (distanceSquared < 1.0E-4F) {
            return;
        }

        float distance = (float) Math.sqrt(distanceSquared);
        toCamera.div(distance);
        float facing = Mth.clamp((flare.forward().dot(toCamera) + 0.15F) / 1.15F, 0.0F, 1.0F);
        facing = facing * facing * (3.0F - 2.0F * facing);

        FlashlightProfile profile = flare.profile();
        float profileIntensity = Mth.clamp(profile.intensity(), 0.25F, 2.0F);
        float visibility = (0.08F + 0.92F * facing)
                * ClientConfig.THIRD_PERSON_FLARE_INTENSITY.get().floatValue()
                * profileIntensity;
        if (visibility <= 0.001F) {
            return;
        }

        float distanceScale = Mth.clamp(1.0F + distance * 0.025F, 1.0F, 3.0F);
        float diameter = ClientConfig.THIRD_PERSON_FLARE_SIZE.get().floatValue() * distanceScale;
        Vector3f cameraRelative = position.sub(
                (float) cameraPosition.x,
                (float) cameraPosition.y,
                (float) cameraPosition.z
        );

        drawQuad(consumer, billboardRotation, cameraRelative, diameter * 0.90F,
                profile.red(), profile.green(), profile.blue(), Mth.clamp(0.20F * visibility, 0.0F, 1.0F));
        drawQuad(consumer, billboardRotation, cameraRelative, diameter * 0.30F,
                profile.red(), profile.green(), profile.blue(), Mth.clamp(0.65F * visibility, 0.0F, 1.0F));
    }

    private static void drawQuad(VertexConsumer consumer, Quaternionf billboardRotation, Vector3f center,
                                 float diameter, float red, float green, float blue, float alpha) {
        float radius = diameter * 0.5F;
        vertex(consumer, corner(center, billboardRotation, radius, -radius), red, green, blue, alpha, 1.0F, 1.0F);
        vertex(consumer, corner(center, billboardRotation, radius, radius), red, green, blue, alpha, 1.0F, 0.0F);
        vertex(consumer, corner(center, billboardRotation, -radius, radius), red, green, blue, alpha, 0.0F, 0.0F);
        vertex(consumer, corner(center, billboardRotation, -radius, -radius), red, green, blue, alpha, 0.0F, 1.0F);
    }

    private static Vector3f corner(Vector3f center, Quaternionf billboardRotation, float x, float y) {
        return new Vector3f(x, y, 0.0F).rotate(billboardRotation).add(center);
    }

    private static void vertex(VertexConsumer consumer, Vector3f position,
                               float red, float green, float blue, float alpha, float u, float v) {
        consumer.addVertex(position)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private FlashlightFlareRenderer() {
    }
}
