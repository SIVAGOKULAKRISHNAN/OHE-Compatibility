package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryHolderBlock;
import com.gokul.ohecompat.block.OhePowerBridgeBlock;
import de.mrjulsen.paw.item.CatenaryWireItem;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the native P&W catenary wire selector explicitly recognize both the
 * CEE catenary holder and our OHE Power Bridge as connector endpoints.
 *
 * The bridge has a native P&W WireConnectorBlockEntity, but an explicit
 * BlockConnectorNodeData path is used here so the wire item cannot reject the
 * bridge during its initial hit test before the graph-side connector mixin
 * gets a chance to supply the exact external endpoint.
 */
@Mixin(CatenaryWireItem.class)
public abstract class PawCatenaryWireItemMixin {
    @Inject(method = "createNodeData", at = @At("HEAD"), cancellable = true)
    private void ohecompat$allowNativeEndpoints(Level level, Player player, InteractionHand hand,
                                                  HitResult hitResult,
                                                  CallbackInfoReturnable<NodeData> cir) {
        BlockPos pos = null;
        if (hitResult instanceof BlockHitResult blockHit) {
            pos = blockHit.getBlockPos();
        } else if (hitResult instanceof de.mrjulsen.paw.util.collision.RaycastHitResult rayHit) {
            pos = rayHit.getBlockPos();
        }

        if (pos == null) return;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof OhePowerBridgeBlock
                || state.getBlock() instanceof CatenaryHolderBlock) {
            cir.setReturnValue(new BlockConnectorNodeData(pos));
            return;
        }

        // The bridge is a three-block visual assembly. If the player targets
        // either internal insulator, resolve the hit to the real P&W
        // WireConnectorBlockEntity on the bridge block two/one blocks above.
        if (state.is(com.gokul.ohecompat.registry.ModBlocks.OHE_SECTION_INSULATOR.get())) {
            BlockPos bridge = null;
            if (level.getBlockState(pos.above()).getBlock() instanceof OhePowerBridgeBlock) {
                bridge = pos.above();
            } else if (level.getBlockState(pos.above(2)).getBlock() instanceof OhePowerBridgeBlock) {
                bridge = pos.above(2);
            }
            if (bridge != null) {
                cir.setReturnValue(new BlockConnectorNodeData(bridge));
            }
        }
    }
}
