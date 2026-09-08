package com.gokul.ohecompat.integration;

import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.george_vi.electroenergetics.simulation.infrastructure.InWorldNodeData;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.gokul.ohecompat.block.TssFeederSwitchBlock;
import com.gokul.ohecompat.blockentity.TssFeederSwitchBlockEntity;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * Makes the P&W-native TSS isolator usable from both sides of the compatibility
 * system without creating another physical wire network.
 *
 * P&W Energy Wire remains the physical conductor. Each TSS terminal gets an
 * invisible CEE shadow node. A closed TSS joins its two shadow terminals; an
 * open TSS removes that internal connection. CEE equipment can attach only at
 * an exact matching CEE node coordinate, never by nearest-node discovery.
 */
@EventBusSubscriber(modid = "ohecompat")
public final class PawTssFeederSwitchManager {
    private static final String FEEDER_PREFIX = "ohecompat:paw:feeder:";
    private static final String TSS_PREFIX = "ohecompat:tss:feeder:";
    private static final double MATCH_EPSILON = 0.08D;

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!com.gokul.ohecompat.config.OheConfig.ENABLE_SHADOW_CEE_NETWORK.get()) return;
        if (!PawBridge.present()) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            syncLevel(level);
        }
    }

    private static void syncLevel(ServerLevel level) {
        WireGraph graph = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);
        if (graph == null) return;

        InfrastructureSavedData sd = InfrastructureSavedData.load(level);
        Set<String> expected = new HashSet<>();

        for (BlockPos pos : TssFeederSwitchBlockEntity.getLoaded(level)) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TssFeederSwitchBlock tss)) continue;

            InWorldNode left = getOrCreateTssNode(sd, pos, 0, worldPosition(pos, tss.endpoint(state.getValue(TssFeederSwitchBlock.FACING), 0)));
            InWorldNode right = getOrCreateTssNode(sd, pos, 1, worldPosition(pos, tss.endpoint(state.getValue(TssFeederSwitchBlock.FACING), 1)));

            if (left == null || right == null) continue;

            // The isolator itself is the only intentional internal connection.
            if (state.getValue(TssFeederSwitchBlock.OPEN)) {
                removeDirect(sd, left, right);
            } else {
                ensureConnected(sd, left, right,
                        Math.max(0.01D, shadowPosition(sd, left).distance(shadowPosition(sd, right))));
                expected.add(internalKey(pos));
            }

            syncExternalEndpoint(level, sd, graph, pos, 0, left, expected);
            syncExternalEndpoint(level, sd, graph, pos, 1, right, expected);
        }

        cleanupStaleTssNodes(level, sd);
    }

    private static void syncExternalEndpoint(ServerLevel level, InfrastructureSavedData sd,
                                              WireGraph graph, BlockPos tssPos, int index,
                                              InWorldNode tssNode, Set<String> expected) {
        Vec3 local = TssFeederSwitchBlock.endpoint(
                level.getBlockState(tssPos).getValue(TssFeederSwitchBlock.FACING), index);
        org.joml.Vector3d endpoint = worldPosition(tssPos, local);

        // First attach to the exact P&W Energy Wire graph endpoint.
        for (WireNode pn : graph.getNodes()) {
            if (!hasFeederEdge(graph, pn.getId())) continue;
            org.joml.Vector3d p = attachmentPosition(pn, null);
            if (p.distance(endpoint) > MATCH_EPSILON) continue;

            InWorldNode feederShadow = getOrCreateFeederShadow(sd, pn, p);
            if (feederShadow == null) continue;
            expected.add(externalKey(tssPos, index, feederShadow));
            ensureConnected(sd, tssNode, feederShadow,
                    Math.max(0.01D, endpoint.distance(p)));
        }

        // Then attach to any CEE node whose electrical endpoint is exactly the
        // same world coordinate. This is what makes the P&W-built TSS usable as
        // the CEE-side feeder isolator as well.
        for (InWorldNode cee : List.copyOf(sd.getNodes())) {
            InWorldNodeData data = sd.getNodeData(cee);
            if (data == null || data.label == null) continue;
            if (data.label.startsWith(TSS_PREFIX) || data.label.startsWith(FEEDER_PREFIX)
                    || data.label.startsWith("ohecompat:paw:ohe:")) continue;
            if (cee.sourcePos().equals(tssPos)) continue;

            Vec3 cp = sd.getNodePositionOrCenter(cee);
            double distance = endpoint.distance(cp.x, cp.y, cp.z);
            if (distance > MATCH_EPSILON) continue;

            expected.add(externalKey(tssPos, index, cee));
            ensureConnected(sd, tssNode, cee, Math.max(0.01D, distance));
        }

        removeUnexpectedExternalLinks(level, sd, tssPos, index, tssNode, expected);
    }

    private static InWorldNode getOrCreateTssNode(InfrastructureSavedData sd, BlockPos pos,
                                                   int index, org.joml.Vector3d world) {
        String label = tssLabel(pos, index);
        for (InWorldNode node : sd.getNodes()) {
            InWorldNodeData data = sd.getNodeData(node);
            if (data != null && label.equals(data.label)) {
                sd.createNode(node, new Vec3(world.x, world.y, world.z), Vec3.ZERO);
                return node;
            }
        }

        InWorldNodeData data = sd.createDetachedNode(
                com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeType.FIXED,
                new Vec3(world.x, world.y, world.z));
        data.label = label;
        return data.node;
    }

    private static InWorldNode getOrCreateFeederShadow(InfrastructureSavedData sd, WireNode pn,
                                                        org.joml.Vector3d world) {
        String label = FEEDER_PREFIX + pn.getId();
        for (InWorldNode node : sd.getNodes()) {
            InWorldNodeData data = sd.getNodeData(node);
            if (data != null && label.equals(data.label)) {
                sd.createNode(node, new Vec3(world.x, world.y, world.z), Vec3.ZERO);
                return node;
            }
        }

        InWorldNodeData data = sd.createDetachedNode(
                com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeType.FIXED,
                new Vec3(world.x, world.y, world.z));
        data.label = label;
        return data.node;
    }

    private static void removeUnexpectedExternalLinks(ServerLevel level, InfrastructureSavedData sd,
                                                       BlockPos tssPos, int index,
                                                       InWorldNode tssNode, Set<String> expected) {
        for (var connection : List.copyOf(sd.getConnections(tssNode))) {
            InWorldNode other = connection.node1().equals(tssNode)
                    ? connection.node2() : connection.node1();
            InWorldNodeData data = sd.getNodeData(other);
            if (data == null || data.label == null) continue;
            if (isTssNode(other, sd)) {
                if (other.id() == tssNode.id()) continue;
                // The internal pair is handled separately by OPEN/CLOSED state.
                continue;
            }
            if (!expected.contains(externalKey(tssPos, index, other))) {
                sd.removeConnectionNoDrops(connection);
            }
        }
    }

    private static void cleanupStaleTssNodes(ServerLevel level, InfrastructureSavedData sd) {
        for (InWorldNode node : List.copyOf(sd.getNodes())) {
            InWorldNodeData data = sd.getNodeData(node);
            if (data == null || data.label == null || !data.label.startsWith(TSS_PREFIX)) continue;

            String[] parts = data.label.substring(TSS_PREFIX.length()).split(":");
            if (parts.length != 2) continue;
            long packed;
            try {
                packed = Long.parseLong(parts[0]);
            } catch (NumberFormatException ignored) {
                sd.removeNode(node);
                continue;
            }
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof TssFeederSwitchBlock)) {
                sd.removeNode(node);
            }
        }
    }

    private static boolean hasFeederEdge(WireGraph graph, UUID nodeId) {
        for (WireEdge edge : graph.getEdges()) {
            if ((edge.getNodeAId().equals(nodeId) || edge.getNodeBId().equals(nodeId))
                    && isFeederWire(edge)) return true;
        }
        return false;
    }

    private static boolean isFeederWire(WireEdge edge) {
        return edge != null && "pantographsandwires:energy_wire".equals(
                edge.getType().getRegistryId().toString());
    }

    private static org.joml.Vector3d attachmentPosition(WireNode node,
                                                          de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider provider) {
        org.joml.Vector3d p = new org.joml.Vector3d(node.getPos());
        if (provider instanceof BasicConnectorDataProvider basic) {
            p.add(basic.getAttachOffset());
        }
        return p;
    }

    private static org.joml.Vector3d worldPosition(BlockPos pos, Vec3 local) {
        return new org.joml.Vector3d(pos.getX() + local.x, pos.getY() + local.y, pos.getZ() + local.z);
    }

    private static org.joml.Vector3d shadowPosition(InfrastructureSavedData sd, InWorldNode node) {
        Vec3 p = sd.getNodePositionOrCenter(node);
        return new org.joml.Vector3d(p.x, p.y, p.z);
    }

    private static String tssLabel(BlockPos pos, int index) {
        return TSS_PREFIX + pos.asLong() + ":" + index;
    }

    private static String internalKey(BlockPos pos) {
        return "internal:" + pos.asLong();
    }

    private static String externalKey(BlockPos pos, int index, InWorldNode other) {
        return pos.asLong() + ":" + index + ":" + other.id();
    }

    private static boolean isTssNode(InWorldNode node, InfrastructureSavedData sd) {
        InWorldNodeData data = sd.getNodeData(node);
        return data != null && data.label != null && data.label.startsWith(TSS_PREFIX);
    }

    private static void ensureConnected(InfrastructureSavedData sd, InWorldNode a, InWorldNode b, double length) {
        for (var connection : sd.getConnections(a)) {
            boolean matches = (connection.node1().equals(a) && connection.node2().equals(b))
                    || (connection.node1().equals(b) && connection.node2().equals(a));
            if (!matches) continue;
            WireData existing = sd.getConnectionData(connection);
            if (existing == null || existing.wireType() != PawShadowWireType.SHADOW.get()) {
                sd.setConnectionData(connection, WireData.ofLength(
                        PawShadowWireType.SHADOW.get(), Math.max(0.01D, length)));
            }
            return;
        }
        sd.connect(a, b, WireData.ofLength(PawShadowWireType.SHADOW.get(), Math.max(0.01D, length)));
    }

    private static void removeDirect(InfrastructureSavedData sd, InWorldNode a, InWorldNode b) {
        for (var connection : List.copyOf(sd.getConnections(a))) {
            if ((connection.node1().equals(a) && connection.node2().equals(b))
                    || (connection.node1().equals(b) && connection.node2().equals(a))) {
                sd.removeConnectionNoDrops(connection);
            }
        }
    }

    private PawTssFeederSwitchManager() {}
}
