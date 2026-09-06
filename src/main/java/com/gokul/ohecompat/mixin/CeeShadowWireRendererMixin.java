package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.client.WireRenderer;
import com.gokul.ohecompat.integration.PawShadowWireType;
import com.george_vi.electroenergetics.simulation.WireType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents CEE's generic wire renderer from drawing the electrical shadow
 * connection that mirrors a P&W OHE edge. The server-side connection remains
 * intact for electrical simulation.
 */
@Mixin(WireRenderer.class)
public abstract class CeeShadowWireRendererMixin {
    @Inject(method = "renderWire", at = @At("HEAD"), cancellable = true)
    private static void ohecompat$hideShadowWire(
            java.util.List<net.minecraft.world.phys.Vec3> points,
            net.minecraft.world.phys.Vec3 start,
            net.minecraft.world.phys.Vec3 end,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            net.minecraft.client.renderer.LevelRenderer levelRenderer,
            WireType wireType,
            net.minecraft.world.level.BlockAndTintGetter level,
            CallbackInfo ci) {
        if (wireType == PawShadowWireType.SHADOW.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "forceRenderWire", at = @At("HEAD"), cancellable = true)
    private static void ohecompat$hideForcedShadowWire(
            java.util.List<net.minecraft.world.phys.Vec3> points,
            net.minecraft.world.phys.Vec3 start,
            net.minecraft.world.phys.Vec3 end,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            WireType wireType,
            net.minecraft.world.level.BlockAndTintGetter level,
            CallbackInfo ci) {
        if (wireType == PawShadowWireType.SHADOW.get()) {
            ci.cancel();
        }
    }
}
