package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryHolderBlock;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Adds P&W BlockConnectorNodeData support to CEE's catenary holder.
 *
 * IMPORTANT: OHE Power Bridge is intentionally NOT special-cased here.
 * It already has the native P&W requirements: IWireConnector,
 * ICatenaryWireConnector and WireConnectorBlockEntity. Therefore P&W's
 * original BlockConnectorNodeData implementation must be allowed to create
 * the bridge node at the block origin and apply the bridge's external face
 * offset through ConnectorDataProvider. Moving the node itself to the face
 * double-applies the offset and makes the wire fail validation.
 */
@Mixin(BlockConnectorNodeData.class)
public abstract class PawCeeHolderBlockConnectorNodeDataMixin {
    @Shadow private boolean pending;

    @Inject(method = "getOrCreateNode", at = @At("HEAD"), cancellable = true)
    private void ohecompat$createCeeNode(WireGraph graph, CallbackInfoReturnable<WireNode> cir) {
        BlockPos pos = ((BlockConnectorNodeData) (Object) this).getPos();
        if (!ohecompat$isCeeHolder(graph.getLevel(), pos)) return;

        this.pending = false;
        BlockState state = graph.getLevel().getBlockState(pos);
        CatenaryHolderBlock holder = (CatenaryHolderBlock) state.getBlock();

        // CEE's catenary holder has its own CEE BlockEntity, not a P&W
        // WireConnectorBlockEntity. Reuse an existing P&W graph node for this
        // block when possible; otherwise create one at CEE's exact node point.
        // OHE Power Bridge does not come through this branch.
        for (WireNode existing : graph.getNodes()) {
            if (existing.getData() instanceof BlockConnectorNodeData data
                    && pos.equals(data.getPos())) {
                cir.setReturnValue(existing);
                return;
            }
        }

        var point = holder.getNodePosition(graph.getLevel(), pos, state, 0);
        WireNode created = graph.createNode((BlockConnectorNodeData) (Object) this,
                new Vector3d(point.x, point.y, point.z));
        cir.setReturnValue(created);
    }

    @Inject(method = "updateWireNode", at = @At("HEAD"), cancellable = true)
    private void ohecompat$updateCeeNode(WireGraph graph, WireNode node,
                                          CallbackInfoReturnable<WireNode> cir) {
        BlockPos pos = ((BlockConnectorNodeData) (Object) this).getPos();
        if (!ohecompat$isCeeHolder(graph.getLevel(), pos)) return;

        BlockState state = graph.getLevel().getBlockState(pos);
        CatenaryHolderBlock holder = (CatenaryHolderBlock) state.getBlock();
        var point = holder.getNodePosition(graph.getLevel(), pos, state, 0);
        node.setPos(new Vector3d(point.x, point.y, point.z));
        this.pending = false;
        cir.setReturnValue(node);
    }

    @Inject(method = "getConnectorCustomData", at = @At("HEAD"), cancellable = true)
    private void ohecompat$ceeConnectorData(IWireGraph graph, CustomData customData, int index,
                                             CallbackInfoReturnable<Optional<ConnectorDataProvider>> cir) {
        BlockPos pos = ((BlockConnectorNodeData) (Object) this).getPos();
        Level level = graph.getLevel();
        if (!ohecompat$isCeeHolder(level, pos)) return;

        this.pending = false;
        IWireConnector connector = (IWireConnector) level.getBlockState(pos).getBlock();
        cir.setReturnValue(Optional.of(connector.getConnectorData(level, pos, customData, index)));
    }

    @Inject(method = "validate", at = @At("HEAD"), cancellable = true)
    private void ohecompat$validateCeeConnector(WireGraph graph,
                                                  de.mrjulsen.paw.components.WireConnectionDataComponent data,
                                                  int index,
                                                  CallbackInfoReturnable<Boolean> cir) {
        BlockPos pos = ((BlockConnectorNodeData) (Object) this).getPos();
        if (ohecompat$isCeeHolder(graph.getLevel(), pos)) {
            this.pending = false;
            cir.setReturnValue(true);
        }
    }

    private static boolean ohecompat$isCeeHolder(Level level, BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof CatenaryHolderBlock
                && state.getBlock() instanceof IWireConnector;
    }
}
