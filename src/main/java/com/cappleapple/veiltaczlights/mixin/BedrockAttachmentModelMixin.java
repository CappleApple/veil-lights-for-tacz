package com.cappleapple.veiltaczlights.mixin;

import com.cappleapple.veiltaczlights.compat.tacz.TaczRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = BedrockAttachmentModel.class, remap = false)
abstract class BedrockAttachmentModelMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void veiltaczlights$begin(@Nullable ItemStack attachmentItem, ItemStack gunItem,
                                      PoseStack poseStack, ItemDisplayContext transformType,
                                      RenderType renderType, int light, int overlay, CallbackInfo ci) {
        TaczRenderContext.begin(attachmentItem, gunItem, transformType);
        TaczRenderContext.captureRoot(poseStack.last().pose());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void veiltaczlights$end(@Nullable ItemStack attachmentItem, ItemStack gunItem,
                                    PoseStack poseStack, ItemDisplayContext transformType,
                                    RenderType renderType, int light, int overlay, CallbackInfo ci) {
        TaczRenderContext.end();
    }
}
