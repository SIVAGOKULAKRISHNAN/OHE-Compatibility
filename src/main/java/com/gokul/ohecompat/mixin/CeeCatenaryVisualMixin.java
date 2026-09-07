package com.gokul.ohecompat.mixin;

import com.gokul.ohecompat.integration.PawShadowWireType;
import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryConnection;
import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryHolderBlock;
import com.george_vi.electroenergetics.simulation.WireType;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps CEE's native catenary visually compatible with the P&W OHE shape.
 *
 * CEE's stock renderer uses the holder block bottom as the wire endpoint and
 * a very thick model. CEE's actual electrical node is higher in the holder.
 * Redirecting the two constructor endpoint calculations makes the rendered
 * line meet the same point exposed to P&W. The native CEE connection remains
 * electrically untouched.
 *
 * If an identical P&W edge is present between the same two holders, CEE's
 * duplicate visual is suppressed so the player sees only the P&W OHE wire.
 */
@Mixin(targets = "com.george_vi.electroenergetics.client.CatenaryVisual")
public abstract class CeeCatenaryVisualMixin {
    @Inject(method = "<init>", at = @At("HEAD"), cancellable = true, require = 0)
    private void ohecompat$hideDuplicatePawVisual(
            VisualizationContext visualizationContext,
            CatenaryConnection connection,
            WireType wireType,
            CallbackInfo ci) {
        // The P&W compatibility graph needs a real CEE connection for electrical
        // simulation, but it is not a physical CEE catenary. CEE's visual is
        // created independently of the wire model, so an empty model alone does
        // not reliably prevent a line from appearing. Cancel only our internal
        // shadow type; native CEE catenaries continue through unchanged.
        if (wireType == PawShadowWireType.SHADOW.get()) {
            ci.cancel();
            return;
        }

        CatenaryConnection c = connection;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        if (ohecompat$hasMatchingPawEdge(level, c.pos1(), c.pos2())) {
            ci.cancel();
        }
    }


    @ModifyConstant(
            method = "<init>",
            constant = @Constant(floatValue = 0.55f),
            require = 0
    )
    private float ohecompat$thinLowerCatenary(float original) {
        return 0.16f;
    }

    @ModifyConstant(
            method = "<init>",
            constant = @Constant(floatValue = 0.35f),
            require = 0
    )
    private float ohecompat$thinUpperCatenary(float original) {
        return 0.10f;
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;atBottomCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            ),
            require = 0
    )
    private Vec3 ohecompat$alignFirstEndpoint(Vec3i pos) {
        return ohecompat$ceeNodeVisualPosition(pos);
    }

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;atBottomCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1
            ),
            require = 0
    )
    private Vec3 ohecompat$alignSecondEndpoint(Vec3i pos) {
        return ohecompat$ceeNodeVisualPosition(pos);
    }

    private static Vec3 ohecompat$ceeNodeVisualPosition(Vec3i pos) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            BlockPos bp = new BlockPos(pos);
            BlockState state = level.getBlockState(bp);
            if (state.getBlock() instanceof CatenaryHolderBlock holder) {
                return holder.getNodePosition(level, bp, state, 0);
            }
        }
        return Vec3.atBottomCenterOf(pos);
    }

    private static boolean ohecompat$hasMatchingPawEdge(Level level, BlockPos aPos, BlockPos bPos) {
        WireGraphClient graph = WireGraphManager.getClient(level, WiresApi.PAW_CATENARY_WIRES);
        if (graph == null) return false;

        for (WireEdge edge : graph.getEdges()) {
            WireNode a = graph.getNode(edge.getNodeAId());
            WireNode b = graph.getNode(edge.getNodeBId());
            if (a == null || b == null) continue;

            if (ohecompat$nodeMatchesHolder(level, a.getPos(), aPos) &&
                    ohecompat$nodeMatchesHolder(level, b.getPos(), bPos)) {
                return true;
            }
            if (ohecompat$nodeMatchesHolder(level, a.getPos(), bPos) &&
                    ohecompat$nodeMatchesHolder(level, b.getPos(), aPos)) {
                return true;
            }
        }
        return false;
    }
    private static boolean ohecompat$nodeMatchesHolder(Level level, org.joml.Vector3d node, BlockPos holderPos) {
        BlockState state = level.getBlockState(holderPos);
        if (!(state.getBlock() instanceof CatenaryHolderBlock holder)) return false;
        Vec3 p = holder.getNodePosition(level, holderPos, state, 0);
        return node.distance(p.x, p.y, p.z) < 0.20D;
    }

}
