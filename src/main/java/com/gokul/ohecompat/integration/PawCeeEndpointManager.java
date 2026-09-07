package com.gokul.ohecompat.integration;

import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.george_vi.electroenergetics.simulation.infrastructure.InWorldNodeData;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeType;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.wires.item.CustomData;
import com.gokul.ohecompat.mixin.PawCantileverAttachPointAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Owns only the CEE-side endpoint used when a native CEE wire is attached to
 * a native P&W cantilever. The P&W cantilever remains the geometry/configuration
 * authority; this node is just an electrical endpoint in CEE's simulation.
 */
public final class PawCeeEndpointManager {
    private static final String LABEL_PREFIX = "ohecompat:paw-cee:";

    private PawCeeEndpointManager() {}

    public static boolean isCantilever(BlockState state) {
        return state != null && state.getBlock() instanceof AbstractCantileverBlock;
    }

    public static Vec3 attachPoint(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isCantilever(state)) return null;
        AbstractCantileverBlock block = (AbstractCantileverBlock) state.getBlock();
        return ((PawCantileverAttachPointAccessor) (Object) block).ohecompat$defaultWireAttachPoint(
                level, pos, state, new CustomData(new net.minecraft.nbt.CompoundTag()), 0);
    }

    public static InWorldNode getOrCreate(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isCantilever(state)) return null;
        InfrastructureSavedData sd = InfrastructureSavedData.load(level);
        String label = LABEL_PREFIX + pos.asLong();
        for (InWorldNode n : sd.getNodes()) {
            InWorldNodeData data = sd.getNodeData(n);
            if (data != null && label.equals(data.label)) {
                update(sd, n, level, pos, state);
                return n;
            }
        }

        Vec3 p = attachPoint(level, pos, state);
        if (p == null) return null;
        InWorldNodeData data = sd.createDetachedNode(DetachedNodeType.FIXED, p);
        data.label = label;
        update(sd, data.node, level, pos, state);
        return data.node;
    }

    public static void updateAll(ServerLevel level) {
        InfrastructureSavedData sd = InfrastructureSavedData.load(level);
        for (InWorldNode n : List.copyOf(sd.getNodes())) {
            InWorldNodeData data = sd.getNodeData(n);
            if (data == null || data.label == null || !data.label.startsWith(LABEL_PREFIX)) continue;
            BlockPos pos = parsePos(data.label.substring(LABEL_PREFIX.length()));
            if (pos == null || !level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!isCantilever(state)) {
                if (sd.getConnections(n).isEmpty()) sd.removeNode(n);
                continue;
            }
            update(sd, n, level, pos, state);
        }
    }

    public static boolean isCompatibilityNode(InWorldNode node, InfrastructureSavedData sd) {
        InWorldNodeData data = sd.getNodeData(node);
        return data != null && data.label != null && data.label.startsWith(LABEL_PREFIX);
    }

    private static void update(InfrastructureSavedData sd, InWorldNode node, ServerLevel level,
                               BlockPos pos, BlockState state) {
        Vec3 p = attachPoint(level, pos, state);
        if (p != null) {
            sd.createNode(node, p, Vec3.ZERO);
        }
    }

    private static BlockPos parsePos(String encoded) {
        try {
            return BlockPos.of(Long.parseLong(encoded));
        } catch (Exception ignored) {
            return null;
        }
    }
}
