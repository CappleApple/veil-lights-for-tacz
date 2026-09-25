package com.cappleapple.veiltaczlights.compat.tacz;

import foundry.veil.impl.client.render.framebuffer.AdvancedFboMutableTextureAttachment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL30C.*;

class ScopeStencilAttachmentTest {
    @Test
    void wrapperPreservesBothStencilBindingAndClearMetadata() {
        var original = new AdvancedFboMutableTextureAttachment(GL_DEPTH_ATTACHMENT, 7, -1, null);
        var fixed = new ScopeStencilAttachment(7);
        assertEquals(0, original.getFormat());
        assertEquals(GL_DEPTH_ATTACHMENT, original.getAttachmentType());
        assertEquals(GL_DEPTH_STENCIL_ATTACHMENT, fixed.getAttachmentType());
        assertEquals(GL_DEPTH_STENCIL, fixed.getFormat());
        assertEquals(original.getId(), fixed.getId());
    }

    @Test
    void cloningAndRetargetingKeepPackedFormatAndBorrowedOwnership() {
        var attachment = new ScopeStencilAttachment(7);
        attachment.setTexture(11, -1);
        var copy = attachment.clone();
        assertNotSame(attachment, copy);
        assertEquals(11, copy.getId());
        assertEquals(GL_DEPTH_STENCIL_ATTACHMENT, copy.getAttachmentType());
        assertEquals(GL_DEPTH_STENCIL, copy.getFormat());
        // A wrapper must not issue a GL deletion, even without a current context.
        copy.releaseId();
        assertEquals(11, attachment.getId());
        assertEquals(11, copy.getId());
    }
}
