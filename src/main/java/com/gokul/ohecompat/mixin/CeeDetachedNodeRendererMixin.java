package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.client.DetachedNodeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CEE detached-node markers are an electrical/debug representation, not a
 * physical OHE component. The compatibility layer therefore keeps the CEE
 * shadow network on the server but never renders detached-node markers.
 *
 * This is intentionally done at the renderer entry point rather than by
 * relying on the node label arriving in the client packet. That makes the
 * suppression deterministic even for nodes loaded from an older world/save.
 */
@Mixin(DetachedNodeRenderer.class)
public abstract class CeeDetachedNodeRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void ohecompat$hideDetachedNodeMarkers(
            net.minecraft.client.multiplayer.ClientLevel level,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            net.minecraft.client.Camera camera,
            CallbackInfo ci) {
        ci.cancel();
    }
}
