package com.cappleapple.veiltaczlights.compat.tacz;

import com.cappleapple.veiltaczlights.config.ClientConfig;
import com.cappleapple.veiltaczlights.config.FlashlightProfileRegistry;
import com.cappleapple.veiltaczlights.lighting.FlashlightProfile;
import com.cappleapple.veiltaczlights.lighting.VeilFlashlightManager;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Bridges TaCZ's render hierarchy to the Veil-facing manager. The context is
 * active only while a BedrockAttachmentModel renders; consequently a captured
 * bone matrix already contains TaCZ's gun, slot, attachment, and animation
 * transforms instead of a reimplementation of them.
 */
public final class TaczRenderContext {
    private static final Pattern AUTOMATIC_EMITTER = Pattern.compile(
            "^(?:flashlight|flash|weapon_light|light|emitter)(?:_illuminated)?(?:_\\d+)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final ThreadLocal<Deque<Context>> CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    public static void begin(ItemStack attachmentStack, ItemStack gunStack, ItemDisplayContext displayContext) {
        IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (attachment == null || gun == null) {
            CONTEXTS.get().push(Context.inactive());
            return;
        }

        ResourceLocation attachmentId = attachment.getAttachmentId(attachmentStack);
        ResourceLocation gunId = gun.getGunId(gunStack);
        boolean firstPerson = displayContext.firstPerson();
        LivingEntity owner = firstPerson ? Minecraft.getInstance().player : RenderOwnerTracker.current();

        Optional<FlashlightProfile> explicit = FlashlightProfileRegistry.explicit(attachmentId);
        boolean allowAutomatic = !FlashlightProfileRegistry.explicitlyDisabled(attachmentId)
                && ClientConfig.AUTOMATIC_BONE_RECOGNITION.get()
                && attachment.getType(attachmentStack) == AttachmentType.LASER;
        CONTEXTS.get().push(new Context(owner, gunId, attachmentId, firstPerson, explicit.orElse(null), allowAutomatic));
    }

    public static void captureRoot(Matrix4f matrix) {
        Context context = current();
        if (context != null && context.profile != null && "@attachment".equals(context.profile.emitterBone())) {
            context.submit(matrix, "@attachment");
        }
    }

    public static void capturePart(String boneName, Matrix4f matrix) {
        if (boneName == null) {
            return;
        }
        Context context = current();
        if (context == null || context.owner == null) {
            return;
        }

        FlashlightProfile profile = context.profile;
        if (profile != null) {
            if (profile.acceptsBone(boneName)) {
                context.submit(matrix, boneName);
            }
            return;
        }
        if (context.allowAutomatic && AUTOMATIC_EMITTER.matcher(boneName.toLowerCase(Locale.ROOT)).matches()) {
            context.profile = FlashlightProfileRegistry.automatic(context.attachmentId, boneName);
            context.submit(matrix, boneName);
        }
    }

    public static void end() {
        Deque<Context> contexts = CONTEXTS.get();
        if (!contexts.isEmpty()) {
            contexts.pop();
        }
        if (contexts.isEmpty()) {
            CONTEXTS.remove();
        }
    }

    private static Context current() {
        return CONTEXTS.get().peek();
    }

    private static final class Context {
        private final LivingEntity owner;
        private final ResourceLocation gunId;
        private final ResourceLocation attachmentId;
        private final boolean firstPerson;
        private FlashlightProfile profile;
        private final boolean allowAutomatic;
        private boolean submitted;

        private Context(LivingEntity owner, ResourceLocation gunId, ResourceLocation attachmentId,
                        boolean firstPerson, FlashlightProfile profile, boolean allowAutomatic) {
            this.owner = owner;
            this.gunId = gunId;
            this.attachmentId = attachmentId;
            this.firstPerson = firstPerson;
            this.profile = profile;
            this.allowAutomatic = allowAutomatic;
        }

        private static Context inactive() {
            return new Context(null, null, null, false, null, false);
        }

        private void submit(Matrix4f matrix, String boneName) {
            // Special TaCZ paths can traverse a bone more than once. The first
            // real model traversal is the stable emitter transform for a frame.
            if (!submitted && owner != null && profile != null) {
                submitted = true;
                VeilFlashlightManager.capture(owner, gunId, attachmentId, firstPerson, profile, matrix, boneName);
            }
        }
    }

    private TaczRenderContext() {
    }
}
