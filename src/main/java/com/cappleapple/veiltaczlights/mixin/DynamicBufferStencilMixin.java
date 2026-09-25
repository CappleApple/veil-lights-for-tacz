package com.cappleapple.veiltaczlights.mixin;

import com.cappleapple.veiltaczlights.compat.tacz.ScopeStencilAttachment;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve TaCZ's scope mask when Veil wraps the first-person target. */
@Mixin(value = DynamicBufferManager.class, remap = false)
abstract class DynamicBufferStencilMixin {
    @Redirect(method = "getDynamicFbo", at = @At(value = "INVOKE",
            target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;setDepthTextureWrapper(I)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;"))
    private AdvancedFbo.Builder veiltaczlights$preserveStencil(
            AdvancedFbo.Builder builder, int textureId, AdvancedFbo source) {
        return source.hasStencilAttachment()
                ? builder.setDepthBuffer(new ScopeStencilAttachment(textureId))
                : builder.setDepthTextureWrapper(textureId);
    }

    @Redirect(method = "getDynamicFbo", at = @At(value = "INVOKE",
            target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo;getWidth()I", ordinal = 0))
    private int veiltaczlights$rejectIncompatiblePooledTarget(AdvancedFbo cached, AdvancedFbo source) {
        // Veil pools by dimensions alone; depth-only and packed depth/stencil
        // targets cannot be substituted for each other even at the same size.
        return cached.hasStencilAttachment() == source.hasStencilAttachment() ? cached.getWidth() : -1;
    }
}
