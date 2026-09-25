package com.cappleapple.veiltaczlights.compat.tacz;

import foundry.veil.impl.client.render.framebuffer.AdvancedFboMutableTextureAttachment;

import static org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL_ATTACHMENT;

/** Non-owning wrapper for Veil's existing packed depth/stencil texture. */
public final class ScopeStencilAttachment extends AdvancedFboMutableTextureAttachment {
    public ScopeStencilAttachment(int textureId) {
        super(GL_DEPTH_STENCIL_ATTACHMENT, textureId, -1, null);
    }

    @Override
    public int getFormat() {
        // Veil uses this metadata to select stencil-aware clears.
        return GL_DEPTH_STENCIL;
    }

    @Override
    public ScopeStencilAttachment clone() {
        return new ScopeStencilAttachment(getId());
    }
}
