package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.client.WireRenderer;
import com.gokul.ohecompat.integration.PawShadowWireType;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNodeConnection;
import com.george_vi.electroenergetics.simulation.WireType;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CEE 1.21.1 uses both immediate rendering and Flywheel WireEffect rendering. Hide the compatibility path in both. */
@Mixin(WireRenderer.class)
public abstract class CeeShadowWireRendererMixin {
    @Inject(method = "addConnection", at = @At("HEAD"), cancellable = true)
    private static void ohecompat$hideShadowConnection(InWorldNodeConnection connection, WireData data, CallbackInfo ci) {
        if (ohecompat$isCompatibility(connection, data)) {
            WireRenderer.removeConnections(connection);
            ci.cancel();
        }
    }

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
        if (wireType == PawShadowWireType.SHADOW.get()) ci.cancel();
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
        if (wireType == PawShadowWireType.SHADOW.get()) ci.cancel();
    }

    private static boolean ohecompat$isCompatibility(InWorldNodeConnection connection, WireData data) {
        if (data != null && data.wireType() == PawShadowWireType.SHADOW.get()) return true;
        return ohecompat$isCompatibilityNode(connection.node1()) || ohecompat$isCompatibilityNode(connection.node2());
    }

    private static boolean ohecompat$isCompatibilityNode(com.george_vi.electroenergetics.foundation.nodes.InWorldNode node) {
        String label = WireRenderer.getNodeLabel(node);
        return label != null && (label.startsWith("ohecompat:paw:")
                || label.startsWith("ohecompat:paw-cee:")
                || label.startsWith("ohecompat:section:"));
    }
}
