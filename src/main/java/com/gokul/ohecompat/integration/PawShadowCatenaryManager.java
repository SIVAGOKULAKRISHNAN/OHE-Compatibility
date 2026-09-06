package com.gokul.ohecompat.integration;

import com.gokul.ohecompat.config.OheConfig;
import com.gokul.ohecompat.blockentity.OheSectionBlockEntity;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.george_vi.electroenergetics.simulation.infrastructure.InWorldNodeData;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryHolderBlock;
import com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = "ohecompat")
public final class PawShadowCatenaryManager {
    private static final String NODE_PREFIX = "ohecompat:paw:";
    private static final String SECTION_PREFIX = "ohecompat:section:";

    private static final Map<ServerLevel, String> SIGNATURES = new WeakHashMap<>();

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!OheConfig.ENABLE_SHADOW_CEE_NETWORK.get()) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            PawCeeEndpointManager.updateAll(level);
            if (PawBridge.present()) sync(level);
        }
    }

    public static void register(net.neoforged.bus.api.IEventBus ignored) {}

    private static void sync(ServerLevel level) {
        WireGraph graph = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);
        if (graph == null) return;

        boolean sectionRefresh = SectionRegistry.refreshUnlinked(level);
        String signature = signature(graph) + "|" + SectionRegistry.signature(level);

        InfrastructureSavedData sd = InfrastructureSavedData.load(level);

        Map<UUID, InWorldNode> nodes = loadExistingPawNodes(sd);
        Set<UUID> livePawNodes = graph.getNodes().stream().map(WireNode::getId).collect(Collectors.toSet());

        // Bridge placement/removal does not change the native P&W graph signature.
        // Therefore exact bridge endpoints must be synchronized even when the
        // graph itself is unchanged. This also cleans links created by older
        // proximity-based bridge code.
        Set<String> expectedBridgeLinks = syncPowerBridgeLinks(level, sd, graph, nodes);
        removeStalePowerBridgeLinks(level, sd, expectedBridgeLinks);

        if (!sectionRefresh && signature.equals(SIGNATURES.get(level))) return;

        // IMPORTANT: a P&W WireNode position is the connector BLOCK position,
        // not necessarily the point where the OHE wire is physically attached.
        // CEE shadow nodes must use the same attachment point as P&W, otherwise
        // the CEE line and the native P&W line appear offset from each other.
        for (WireNode pn : graph.getNodes()) {
            InWorldNode n = nodes.get(pn.getId());
            org.joml.Vector3d attach = attachmentPosition(pn, null);
            if (n == null || !sd.hasNode(n)) {
                InWorldNodeData d = sd.createDetachedNode(DetachedNodeType.FIXED,
                        new net.minecraft.world.phys.Vec3(attach.x, attach.y, attach.z));
                d.label = NODE_PREFIX + pn.getId();
                nodes.put(pn.getId(), d.node);
            } else {
                sd.createNode(n, new net.minecraft.world.phys.Vec3(attach.x, attach.y, attach.z),
                        net.minecraft.world.phys.Vec3.ZERO);
            }

            // If the endpoint is not our OHE Power Bridge, it may be a native
            // CEE railway catenary holder. That compatibility path remains exact
            // position matching and does not search for nearby unrelated nodes.
            if (!isPowerBridgeEndpoint(level, pn)) {
                bridgeToCeeHolder(level, sd, pn, nodes.get(pn.getId()));
            }
        }

        Set<NodePair> expected = new HashSet<>();
        for (WireEdge edge : graph.getEdges()) {
            WireNode a = graph.getNode(edge.getNodeAId());
            WireNode b = graph.getNode(edge.getNodeBId());
            if (a == null || b == null) continue;
            InWorldNode ca = nodes.get(a.getId());
            InWorldNode cb = nodes.get(b.getId());
            if (ca == null || cb == null) continue;

            org.joml.Vector3d pa = attachmentPosition(a, edge.getWireConnectionData().connectorA());
            org.joml.Vector3d pb = attachmentPosition(b, edge.getWireConnectionData().connectorB());
            // Keep the CEE electrical wire exactly between the P&W OHE
            // attachment points. Do not use the connector block centers.
            sd.createNode(ca, new net.minecraft.world.phys.Vec3(pa.x, pa.y, pa.z),
                    net.minecraft.world.phys.Vec3.ZERO);
            sd.createNode(cb, new net.minecraft.world.phys.Vec3(pb.x, pb.y, pb.z),
                    net.minecraft.world.phys.Vec3.ZERO);

            expected.add(new NodePair(ca.id(), cb.id()));
            ensureConnected(sd, ca, cb, distance(pa, pb));
        }

        // Remove stale shadow connections and old direct connections replaced by sections.
        for (InWorldNode n : sd.getNodes()) {
            InWorldNodeData nd = sd.getNodeData(n);
            if (nd == null || nd.label == null || !nd.label.startsWith(NODE_PREFIX)) continue;
            for (var c : List.copyOf(sd.getConnections(nd))) {
                InWorldNode a = c.node1();
                InWorldNode b = c.node2();
                if (!isPawNode(a, sd) || !isPawNode(b, sd)) continue;
                if (!expected.contains(new NodePair(a.id(), b.id()))) {
                    sd.removeConnectionNoDrops(c);
                }
            }
        }

        // Remove old compatibility-created section endpoints/connections first.
        // This prevents stale left/right nodes when a section is reconnected or removed.
        cleanupSectionNodes(sd);

        // Apply section gaps to the electrical shadow while leaving P&W geometry intact.
        for (OheSectionBlockEntity section : SectionRegistry.get(level)) {
            if (!section.isolated() || section.edgeId() == null) continue;
            WireEdge edge = graph.getEdge(section.edgeId());
            if (edge == null) continue;
            WireNode a = graph.getNode(edge.getNodeAId());
            WireNode b = graph.getNode(edge.getNodeBId());
            if (a == null || b == null) continue;
            InWorldNode ca = nodes.get(a.getId());
            InWorldNode cb = nodes.get(b.getId());
            if (ca == null || cb == null) continue;

            // Remove the original electrical connection across the physical neutral section.
            removeDirect(sd, ca, cb);

            // Section percentage is measured along the actual P&W OHE wire,
            // so use the same physical attachment points as the native wire.
            org.joml.Vector3d pa = attachmentPosition(a, edge.getWireConnectionData().connectorA());
            org.joml.Vector3d pb = attachmentPosition(b, edge.getWireConnectionData().connectorB());
            org.joml.Vector3d mid = midpoint(pa, pb, section.percentage());
            org.joml.Vector3d gapAxis = new org.joml.Vector3d(pb).sub(pa).normalize();
            gapAxis.mul(0.08);
            InWorldNode left = sectionNode(sd, section.getBlockPos(), "L", new org.joml.Vector3d(mid).sub(gapAxis));
            InWorldNode right = sectionNode(sd, section.getBlockPos(), "R", new org.joml.Vector3d(mid).add(gapAxis));
            ensureConnected(sd, ca, left, distance(pa, mid));
            ensureConnected(sd, right, cb, distance(mid, pb));
            // Intentionally no left-right connection: this is the electrical dead gap.
        }

        // Remove orphan P&W shadow nodes. Their connections are removed first.
        for (InWorldNode n : List.copyOf(sd.getNodes())) {
            InWorldNodeData nd = sd.getNodeData(n);
            if (nd != null && nd.label != null && nd.label.startsWith(NODE_PREFIX)) {
                UUID id = parseUuid(nd.label.substring(NODE_PREFIX.length()));
                if (id != null && !livePawNodes.contains(id)) {
                    sd.removeNode(n);
                }
            }
        }

        boolean allSectionsLinked = SectionRegistry.get(level).stream()
                .allMatch(section -> section.edgeId() != null);
        if (allSectionsLinked) {
            SIGNATURES.put(level, signature);
        } else {
            SIGNATURES.remove(level);
        }
    }

    private static Map<UUID, InWorldNode> loadExistingPawNodes(InfrastructureSavedData sd) {
        Map<UUID, InWorldNode> result = new HashMap<>();
        for (InWorldNode n : sd.getNodes()) {
            InWorldNodeData d = sd.getNodeData(n);
            if (d == null || d.label == null || !d.label.startsWith(NODE_PREFIX)) continue;
            UUID id = parseUuid(d.label.substring(NODE_PREFIX.length()));
            if (id != null) result.put(id, n);
        }
        return result;
    }

    private static InWorldNode sectionNode(InfrastructureSavedData sd, net.minecraft.core.BlockPos pos,
                                           String side, org.joml.Vector3d p) {
        String label = SECTION_PREFIX + pos.asLong() + ":" + side;
        for (InWorldNode n : sd.getNodes()) {
            InWorldNodeData d = sd.getNodeData(n);
            if (d != null && label.equals(d.label)) {
                sd.createNode(n, new net.minecraft.world.phys.Vec3(p.x, p.y, p.z), net.minecraft.world.phys.Vec3.ZERO);
                return n;
            }
        }
        InWorldNodeData d = sd.createDetachedNode(DetachedNodeType.FIXED,
                new net.minecraft.world.phys.Vec3(p.x, p.y, p.z));
        d.label = label;
        return d.node;
    }

    private static Set<String> syncPowerBridgeLinks(ServerLevel level, InfrastructureSavedData sd,
                                                     WireGraph graph, Map<UUID, InWorldNode> nodes) {
        Set<String> expected = new HashSet<>();
        for (WireNode pn : graph.getNodes()) {
            InWorldNode shadow = nodes.get(pn.getId());
            if (shadow == null) continue;
            net.minecraft.core.BlockPos bridgePos = findPowerBridgeAtEndpoint(level, attachmentPosition(pn, null));
            if (bridgePos == null) continue;

            sd.registerOrUpdateNodes(bridgePos, List.of(0));
            InWorldNode ceeNode = new InWorldNode(0, bridgePos);
            if (!sd.hasNode(ceeNode)) continue;

            expected.add(bridgeLinkKey(shadow, bridgePos));
            org.joml.Vector3d p = attachmentPosition(pn, null);
            net.minecraft.world.phys.Vec3 cp = sd.getNodePositionOrCenter(ceeNode);
            ensureConnected(sd, shadow, ceeNode, Math.max(0.01D, p.distance(cp.x, cp.y, cp.z)));
        }
        return expected;
    }

    private static void removeStalePowerBridgeLinks(ServerLevel level, InfrastructureSavedData sd,
                                                     Set<String> expected) {
        for (InWorldNode shadow : List.copyOf(sd.getNodes())) {
            if (!isPawNode(shadow, sd)) continue;
            for (var connection : List.copyOf(sd.getConnections(shadow))) {
                InWorldNode other = connection.node1().equals(shadow)
                        ? connection.node2() : connection.node1();
                if (other.id() != 0) continue;
                BlockPos pos = other.sourcePos();
                if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock()
                        instanceof com.gokul.ohecompat.block.OhePowerBridgeBlock)) continue;
                if (!expected.contains(bridgeLinkKey(shadow, pos))) {
                    sd.removeConnectionNoDrops(connection);
                }
            }
        }
    }

    private static String bridgeLinkKey(InWorldNode shadow, BlockPos bridgePos) {
        return shadow.id() + ":" + bridgePos.asLong();
    }

    private static boolean isPowerBridgeEndpoint(ServerLevel level, WireNode pn) {
        return findPowerBridgeAtEndpoint(level, attachmentPosition(pn, null)) != null;
    }

    private static BlockPos findPowerBridgeAtEndpoint(ServerLevel level, org.joml.Vector3d endpoint) {
        BlockPos base = BlockPos.containing(endpoint.x, endpoint.y, endpoint.z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = base.offset(dx, dy, dz);
                    if (!level.isLoaded(candidate)) continue;
                    BlockState state = level.getBlockState(candidate);
                    if (!(state.getBlock() instanceof com.gokul.ohecompat.block.OhePowerBridgeBlock bridge)) continue;
                    net.minecraft.world.phys.Vec3 connector = bridge.tensionWireAttachPoint(
                            level, candidate, state, new de.mrjulsen.wires.item.CustomData(new net.minecraft.nbt.CompoundTag()), 0);
                    org.joml.Vector3d expected = new org.joml.Vector3d(
                            candidate.getX() + connector.x, candidate.getY() + connector.y, candidate.getZ() + connector.z);
                    if (endpoint.distance(expected) <= 0.08D) return candidate;
                }
            }
        }
        return null;
    }

    private static void bridgeToCeeHolder(ServerLevel level, InfrastructureSavedData sd,
                                           WireNode pawNode, InWorldNode shadowNode) {
        if (shadowNode == null) return;

        org.joml.Vector3d nodePos = new org.joml.Vector3d(pawNode.getPos());

        // Fixed13 moved the P&W WireNode to CEE's actual contact point. That
        // point is above the CEE holder's block origin, so simply flooring the
        // Y coordinate can select the block above the holder. Find the native
        // CEE holder whose node position actually matches the P&W endpoint.
        net.minecraft.core.BlockPos base = net.minecraft.core.BlockPos.containing(
                nodePos.x, nodePos.y, nodePos.z);
        net.minecraft.core.BlockPos holderPos = null;
        CatenaryHolderBlock holder = null;
        net.minecraft.world.level.block.state.BlockState holderState = null;
        double best = 0.20D;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    net.minecraft.core.BlockPos candidate = base.offset(dx, dy, dz);
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(candidate);
                    if (!(state.getBlock() instanceof CatenaryHolderBlock h)) continue;
                    net.minecraft.world.phys.Vec3 cp = h.getNodePosition(level, candidate, state, 0);
                    double d = nodePos.distance(cp.x, cp.y, cp.z);
                    if (d < best) {
                        best = d;
                        holderPos = candidate;
                        holder = h;
                        holderState = state;
                    }
                }
            }
        }
        if (holderPos == null || holder == null || holderState == null) return;

        // Ensure CEE has its native electrical node registered before we bridge to it.
        sd.registerOrUpdateNodes(holderPos, List.of(0));
        InWorldNode ceeNode = new InWorldNode(0, holderPos);
        if (!sd.hasNode(ceeNode)) return;

        net.minecraft.world.phys.Vec3 cp = holder.getNodePosition(level, holderPos, holderState, 0);
        org.joml.Vector3d c = new org.joml.Vector3d(cp.x, cp.y, cp.z);
        sd.createNode(ceeNode, new net.minecraft.world.phys.Vec3(c.x, c.y, c.z),
                net.minecraft.world.phys.Vec3.ZERO);
        ensureConnected(sd, shadowNode, ceeNode, Math.max(0.01, shadowNodePosition(sd, shadowNode).distance(c)));
    }

    private static org.joml.Vector3d shadowNodePosition(InfrastructureSavedData sd, InWorldNode node) {
        net.minecraft.world.phys.Vec3 p = sd.getNodePositionOrCenter(node);
        return new org.joml.Vector3d(p.x, p.y, p.z);
    }

    private static void ensureConnected(InfrastructureSavedData sd, InWorldNode a, InWorldNode b, double length) {
        for (var connection : sd.getConnections(a)) {
            boolean matches = (connection.node1().equals(a) && connection.node2().equals(b))
                    || (connection.node1().equals(b) && connection.node2().equals(a));
            if (!matches) continue;

            // Worlds created with earlier fixes can already contain this same
            // compatibility connection saved as CEE's visible standard wire.
            // Keep the electrical link, but migrate its data to the internal
            // shadow type so the client-side shadow-wire filter can hide it.
            WireData existing = sd.getConnectionData(connection);
            if (existing == null || existing.wireType() != PawShadowWireType.SHADOW.get()) {
                sd.setConnectionData(connection, WireData.ofLength(
                        PawShadowWireType.SHADOW.get(), Math.max(0.01, length)));
            }
            return;
        }

        sd.connect(a, b, WireData.ofLength(PawShadowWireType.SHADOW.get(), Math.max(0.01, length)));
    }

    private static void removeDirect(InfrastructureSavedData sd, InWorldNode a, InWorldNode b) {
        for (var c : List.copyOf(sd.getConnections(a))) {
            if ((c.node1().equals(a) && c.node2().equals(b)) ||
                    (c.node1().equals(b) && c.node2().equals(a))) {
                sd.removeConnectionNoDrops(c);
            }
        }
    }

    private static boolean isPawNode(InWorldNode n, InfrastructureSavedData sd) {
        InWorldNodeData d = sd.getNodeData(n);
        return d != null && d.label != null && d.label.startsWith(NODE_PREFIX);
    }

    private static void cleanupSectionNodes(InfrastructureSavedData sd) {
        for (InWorldNode n : List.copyOf(sd.getNodes())) {
            InWorldNodeData d = sd.getNodeData(n);
            if (d != null && d.label != null && d.label.startsWith(SECTION_PREFIX)) {
                sd.removeNode(n);
            }
        }
    }

    private static String signature(WireGraph graph) {
        return graph.getEdges().stream()
                .sorted(Comparator.comparing(e -> e.getId().toString()))
                .map(e -> {
                    WireNode a = graph.getNode(e.getNodeAId());
                    WireNode b = graph.getNode(e.getNodeBId());
                    org.joml.Vector3d pa = a == null ? new org.joml.Vector3d()
                            : attachmentPosition(a, e.getWireConnectionData().connectorA());
                    org.joml.Vector3d pb = b == null ? new org.joml.Vector3d()
                            : attachmentPosition(b, e.getWireConnectionData().connectorB());
                    return e.getId() + ":" + e.getNodeAId() + ":" + e.getNodeBId() +
                            ":" + pa.x + "," + pa.y + "," + pa.z +
                            ":" + pb.x + "," + pb.y + "," + pb.z;
                })
                .collect(Collectors.joining("|"));
    }

    /**
     * Returns the physical OHE attachment point used by P&W.
     *
     * P&W's WireNode#getPos() identifies the connector block/node. The actual
     * wire endpoint is node position + BasicConnectorDataProvider#getAttachOffset().
     * Using this value is essential for cantilevers and other offset connectors.
     */
    private static org.joml.Vector3d attachmentPosition(WireNode node, ConnectorDataProvider provider) {
        org.joml.Vector3d p = new org.joml.Vector3d(node.getPos());
        if (provider instanceof BasicConnectorDataProvider basic) {
            p.add(basic.getAttachOffset());
        }
        return p;
    }

    private static org.joml.Vector3d midpoint(org.joml.Vector3d a, org.joml.Vector3d b, double pct) {
        return new org.joml.Vector3d(a).lerp(b, Math.max(0.02, Math.min(0.98, pct)));
    }

    private static double distance(org.joml.Vector3d a, org.joml.Vector3d b) {
        return a.distance(b);
    }

    private static UUID parseUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception ignored) { return null; }
    }

    private record NodePair(int a, int b) {
        NodePair {
            if (a > b) {
                int t = a; a = b; b = t;
            }
        }
    }

    private PawShadowCatenaryManager() {}
}
