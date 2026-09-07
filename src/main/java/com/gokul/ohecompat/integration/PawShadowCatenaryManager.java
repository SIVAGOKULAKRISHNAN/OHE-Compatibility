package com.gokul.ohecompat.integration;

import com.gokul.ohecompat.config.OheConfig;
import com.gokul.ohecompat.blockentity.OheSectionBlockEntity;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.george_vi.electroenergetics.simulation.infrastructure.InWorldNodeData;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryHolderBlock;
import com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchBlock;
import com.gokul.ohecompat.block.OheSectionAssemblyBlock;
import com.gokul.ohecompat.block.OheSwitchAssemblyBlock;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = "ohecompat")
public final class PawShadowCatenaryManager {
    private static final String NODE_PREFIX = "ohecompat:paw:ohe:";
    private static final String FEEDER_NODE_PREFIX = "ohecompat:paw:feeder:";
    private static final String SECTION_PREFIX = "ohecompat:section:";

    private static final Map<ServerLevel, String> SIGNATURES = new WeakHashMap<>();

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!OheConfig.ENABLE_SHADOW_CEE_NETWORK.get()) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            PawCeeEndpointManager.updateAll(level);
            if (CeeBridge.present()) syncSectionCeeContinuity(level);
            if (PawBridge.present()) sync(level);
        }
    }

    public static void register(net.neoforged.bus.api.IEventBus ignored) {}

    /** Keep C1-C4 CEE-side continuity usable even in a CEE-only installation.
     * The connection is invisible because it is a compatibility shadow wire.
     */
    private static void syncSectionCeeContinuity(ServerLevel level) {
        InfrastructureSavedData sd = InfrastructureSavedData.load(level);
        for (OheSectionBlockEntity section : SectionRegistry.get(level)) {
            BlockPos pos = section.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof OheSectionAssemblyBlock block)) continue;
            sd.registerOrUpdateNodes(pos, List.of(0, 1));
            InWorldNode a = new InWorldNode(0, pos);
            InWorldNode b = new InWorldNode(1, pos);
            if (!sd.hasNode(a) || !sd.hasNode(b)) continue;
            if (state.getValue(OheSectionAssemblyBlock.ISOLATED)) removeDirect(sd, a, b);
            else ensureConnected(sd, a, b, Math.max(0.01,
                    sd.getNodePositionOrCenter(a).distanceTo(sd.getNodePositionOrCenter(b))));
        }
    }

    private static void sync(ServerLevel level) {
        WireGraph graph = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);
        if (graph == null) return;

        boolean sectionRefresh = SectionRegistry.refreshUnlinked(level);
        String signature = signature(graph) + "|" + SectionRegistry.signature(level);

        InfrastructureSavedData sd = InfrastructureSavedData.load(level);

        Map<UUID, InWorldNode> nodes = loadExistingPawNodes(sd, NODE_PREFIX);
        Map<UUID, InWorldNode> feederNodes = loadExistingPawNodes(sd, FEEDER_NODE_PREFIX);
        Set<UUID> livePawNodes = new HashSet<>();
        Set<UUID> liveFeederNodes = new HashSet<>();
        for (WireEdge edge : graph.getEdges()) {
            if (isOheWire(edge)) {
                livePawNodes.add(edge.getNodeAId());
                livePawNodes.add(edge.getNodeBId());
            } else if (isFeederWire(edge)) {
                liveFeederNodes.add(edge.getNodeAId());
                liveFeederNodes.add(edge.getNodeBId());
            }
        }

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
            // A P&W node can physically host more than one wire type. Keep
            // separate CEE shadow nodes so OHE and feeder never become one
            // electrical circuit merely because they share a P&W connector.
            if (!hasOheEdge(graph, pn.getId())) continue;
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
            if (!isOheWire(edge)) continue;
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

        // C1-C4 expose two real CEE connector nodes. If a native P&W node is
        // physically attached to either endpoint, bridge that exact endpoint to
        // the matching CEE node. When the section is not isolated, connect its
        // two CEE nodes with an invisible shadow wire; when isolated, leave the
        // two sides electrically open. Native P&W geometry is never modified.
        // Mirror P&W's native Energy Wire (Power Line / feeder) into a
        // separate invisible CEE electrical circuit. The physical conductor,
        // rendering and node ownership remain entirely P&W-side.
        syncFeederNetwork(level, sd, graph, feederNodes);
        Set<String> expectedFeederBridgeLinks = syncFeederBridgeLinks(level, sd, graph, feederNodes);
        removeStaleFeederBridgeLinks(level, sd, expectedFeederBridgeLinks);
        Set<String> expectedFeederSwitchLinks = syncFeederSwitchCompatibilityLinks(level, sd, graph, feederNodes);
        removeStaleFeederSwitchCompatibilityLinks(level, sd, expectedFeederSwitchLinks);
        removeStaleFeederShadowConnections(sd, liveFeederNodes);
        cleanupOrphanShadowNodes(sd, FEEDER_NODE_PREFIX, liveFeederNodes);

        Set<String> expectedSectionLinks = syncSectionCompatibilityLinks(level, sd, graph);
        removeStaleSectionCompatibilityLinks(level, sd, expectedSectionLinks);

        // D is only a railway mounting/operating shell. Its electrical authority
        // remains the already-placed CEE HV switch. Match P&W endpoints to the
        // exact native HV-switch node coordinates; do not use nearest-node
        // discovery or distance-based electrical linking.
        Set<String> expectedSwitchLinks = syncSwitchCompatibilityLinks(level, sd, graph);
        removeStaleSwitchCompatibilityLinks(level, sd, expectedSwitchLinks);

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

    private static boolean isOheWire(WireEdge edge) {
        return edge != null && "pantographsandwires:catenary_wire".equals(
                edge.getType().getRegistryId().toString());
    }

    private static boolean isFeederWire(WireEdge edge) {
        return edge != null && "pantographsandwires:energy_wire".equals(
                edge.getType().getRegistryId().toString());
    }

    private static boolean hasOheEdge(WireGraph graph, UUID nodeId) {
        for (WireEdge edge : graph.getEdges()) {
            if ((edge.getNodeAId().equals(nodeId) || edge.getNodeBId().equals(nodeId)) && isOheWire(edge)) {
                return true;
            }
        }
        return false;
    }

    private static void syncFeederNetwork(ServerLevel level, InfrastructureSavedData sd,
                                          WireGraph graph, Map<UUID, InWorldNode> nodes) {
        for (WireEdge edge : graph.getEdges()) {
            if (!isFeederWire(edge)) continue;
            WireNode a = graph.getNode(edge.getNodeAId());
            WireNode b = graph.getNode(edge.getNodeBId());
            if (a == null || b == null) continue;

            InWorldNode ca = getOrCreateShadowNode(sd, nodes, a, FEEDER_NODE_PREFIX);
            InWorldNode cb = getOrCreateShadowNode(sd, nodes, b, FEEDER_NODE_PREFIX);
            if (ca == null || cb == null) continue;

            org.joml.Vector3d pa = attachmentPosition(a, edge.getWireConnectionData().connectorA());
            org.joml.Vector3d pb = attachmentPosition(b, edge.getWireConnectionData().connectorB());
            sd.createNode(ca, new Vec3(pa.x, pa.y, pa.z), Vec3.ZERO);
            sd.createNode(cb, new Vec3(pb.x, pb.y, pb.z), Vec3.ZERO);
            ensureConnected(sd, ca, cb, distance(pa, pb));
        }
    }

    private static InWorldNode getOrCreateShadowNode(InfrastructureSavedData sd,
                                                      Map<UUID, InWorldNode> nodes,
                                                      WireNode pn, String prefix) {
        InWorldNode n = nodes.get(pn.getId());
        org.joml.Vector3d p = attachmentPosition(pn, null);
        if (n == null || !sd.hasNode(n)) {
            InWorldNodeData d = sd.createDetachedNode(DetachedNodeType.FIXED,
                    new Vec3(p.x, p.y, p.z));
            d.label = prefix + pn.getId();
            n = d.node;
            nodes.put(pn.getId(), n);
        } else {
            sd.createNode(n, new Vec3(p.x, p.y, p.z), Vec3.ZERO);
        }
        return n;
    }

    private static Set<String> syncFeederSwitchCompatibilityLinks(ServerLevel level,
                                                                    InfrastructureSavedData sd,
                                                                    WireGraph graph,
                                                                    Map<UUID, InWorldNode> feederNodes) {
        Set<String> expected = new HashSet<>();
        for (WireNode pn : graph.getNodes()) {
            if (!hasFeederEdge(graph, pn.getId())) continue;
            InWorldNode shadow = feederNodes.get(pn.getId());
            if (shadow == null) continue;
            org.joml.Vector3d endpoint = attachmentPosition(pn, null);
            BlockPos base = BlockPos.containing(endpoint.x, endpoint.y, endpoint.z);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos assemblyPos = base.offset(dx, dy, dz);
                        BlockState assemblyState = level.getBlockState(assemblyPos);
                        if (!(assemblyState.getBlock() instanceof OheSwitchAssemblyBlock)) continue;
                        if (!assemblyState.getValue(OheSwitchAssemblyBlock.FEEDER_MODE)) continue;
                        BlockPos switchPos = findExactHvSwitchNode(level, endpoint, assemblyPos);
                        if (switchPos == null) continue;
                        BlockState switchState = level.getBlockState(switchPos);
                        if (!(switchState.getBlock() instanceof HVSwitchBlock hv)) continue;
                        sd.registerOrUpdateNodes(switchPos, List.of(0, 1));
                        for (int nodeIndex = 0; nodeIndex < 2; nodeIndex++) {
                            Vec3 np = hv.getNodePosition(level, switchPos, switchState, nodeIndex);
                            org.joml.Vector3d world = new org.joml.Vector3d(
                                    switchPos.getX()+np.x, switchPos.getY()+np.y, switchPos.getZ()+np.z);
                            if (endpoint.distance(world) > 0.08D) continue;
                            InWorldNode cee = new InWorldNode(nodeIndex, switchPos);
                            if (!sd.hasNode(cee)) continue;
                            sd.createNode(cee, new Vec3(world.x, world.y, world.z), Vec3.ZERO);
                            expected.add(switchLinkKey(switchPos, nodeIndex, shadow));
                            ensureConnected(sd, shadow, cee,
                                    Math.max(0.01, endpoint.distance(world)));
                        }
                    }
                }
            }
        }
        return expected;
    }

    private static boolean hasFeederEdge(WireGraph graph, UUID nodeId) {
        for (WireEdge edge : graph.getEdges()) {
            if ((edge.getNodeAId().equals(nodeId) || edge.getNodeBId().equals(nodeId)) && isFeederWire(edge))
                return true;
        }
        return false;
    }

    private static void removeStaleFeederSwitchCompatibilityLinks(ServerLevel level,
                                                                    InfrastructureSavedData sd,
                                                                    Set<String> expected) {
        for (InWorldNode node : List.copyOf(sd.getNodes())) {
            InWorldNodeData data = sd.getNodeData(node);
            if (data == null || data.label == null || !data.label.startsWith(FEEDER_NODE_PREFIX)) continue;
            for (var connection : List.copyOf(sd.getConnections(node))) {
                InWorldNode other = connection.node1().equals(node) ? connection.node2() : connection.node1();
                if (other.id() < 0) continue;
                BlockPos source = other.sourcePos();
                if (!level.isLoaded(source)) continue;
                if (!(level.getBlockState(source).getBlock() instanceof HVSwitchBlock)) continue;
                int index = other.id();
                if (!expected.contains(switchLinkKey(source, index, node))) {
                    sd.removeConnectionNoDrops(connection);
                }
            }
        }
    }

    private static void removeStaleFeederShadowConnections(InfrastructureSavedData sd, Set<UUID> live) {
        for (InWorldNode n : List.copyOf(sd.getNodes())) {
            InWorldNodeData d = sd.getNodeData(n);
            if (d == null || d.label == null || !d.label.startsWith(FEEDER_NODE_PREFIX)) continue;
            UUID id = parseUuid(d.label.substring(FEEDER_NODE_PREFIX.length()));
            for (var c : List.copyOf(sd.getConnections(n))) {
                InWorldNode other = c.node1().equals(n) ? c.node2() : c.node1();
                InWorldNodeData od = sd.getNodeData(other);
                UUID oid = od != null && od.label != null && od.label.startsWith(FEEDER_NODE_PREFIX)
                        ? parseUuid(od.label.substring(FEEDER_NODE_PREFIX.length())) : null;
                if (od != null && od.label != null && od.label.startsWith(FEEDER_NODE_PREFIX)
                        && (!live.contains(id) || !live.contains(oid))) {
                    sd.removeConnectionNoDrops(c);
                }
            }
        }
    }

    private static void cleanupOrphanShadowNodes(InfrastructureSavedData sd, String prefix, Set<UUID> live) {
        for (InWorldNode n : List.copyOf(sd.getNodes())) {
            InWorldNodeData d = sd.getNodeData(n);
            if (d == null || d.label == null || !d.label.startsWith(prefix)) continue;
            UUID id = parseUuid(d.label.substring(prefix.length()));
            if (id != null && !live.contains(id) && sd.getConnections(n).isEmpty()) sd.removeNode(n);
        }
    }

    private static Map<UUID, InWorldNode> loadExistingPawNodes(InfrastructureSavedData sd, String prefix) {
        Map<UUID, InWorldNode> result = new HashMap<>();
        for (InWorldNode n : sd.getNodes()) {
            InWorldNodeData d = sd.getNodeData(n);
            if (d == null || d.label == null || !d.label.startsWith(prefix)) continue;
            UUID id = parseUuid(d.label.substring(prefix.length()));
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

    private static Set<String> syncSectionCompatibilityLinks(ServerLevel level,
                                                               InfrastructureSavedData sd,
                                                               WireGraph graph) {
        Set<String> expected = new HashSet<>();
        for (OheSectionBlockEntity section : SectionRegistry.get(level)) {
            BlockPos pos = section.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof OheSectionAssemblyBlock block)) continue;

            sd.registerOrUpdateNodes(pos, List.of(0, 1));
            InWorldNode left = new InWorldNode(0, pos);
            InWorldNode right = new InWorldNode(1, pos);
            if (!sd.hasNode(left) || !sd.hasNode(right)) continue;

            sd.createNode(left, new net.minecraft.world.phys.Vec3(
                    pos.getX() + block.getNodePosition(level, pos, state, 0).x,
                    pos.getY() + block.getNodePosition(level, pos, state, 0).y,
                    pos.getZ() + block.getNodePosition(level, pos, state, 0).z),
                    net.minecraft.world.phys.Vec3.ZERO);
            sd.createNode(right, new net.minecraft.world.phys.Vec3(
                    pos.getX() + block.getNodePosition(level, pos, state, 1).x,
                    pos.getY() + block.getNodePosition(level, pos, state, 1).y,
                    pos.getZ() + block.getNodePosition(level, pos, state, 1).z),
                    net.minecraft.world.phys.Vec3.ZERO);

            // If closed/connected, electrically join the two native CEE nodes.
            // The shadow wire type keeps this compatibility-only connection invisible.
            if (!state.getValue(OheSectionAssemblyBlock.ISOLATED)) {
                ensureConnected(sd, left, right,
                        Math.max(0.01, sd.getNodePositionOrCenter(left).distanceTo(sd.getNodePositionOrCenter(right))));
            } else {
                removeDirect(sd, left, right);
            }

            for (WireNode pn : graph.getNodes()) {
                org.joml.Vector3d p = attachmentPosition(pn, null);
                int match = -1;
                for (int idx = 0; idx < 2; idx++) {
                    Vec3 ep = block.getNodePosition(level, pos, state, idx);
                    org.joml.Vector3d world = new org.joml.Vector3d(pos.getX()+ep.x, pos.getY()+ep.y, pos.getZ()+ep.z);
                    if (p.distance(world) <= 0.08D) { match = idx; break; }
                }
                if (match < 0) continue;
                InWorldNode shadow = nodesFor(sd, pn.getId());
                if (shadow == null) continue;
                InWorldNode cee = match == 0 ? left : right;
                expected.add(sectionLinkKey(pos, match, shadow));
                ensureConnected(sd, shadow, cee, Math.max(0.01, shadowNodePosition(sd, shadow).distance(shadowNodePosition(sd, cee))));
            }
        }
        return expected;
    }

    private static InWorldNode nodesFor(InfrastructureSavedData sd, UUID id) {
        String label = NODE_PREFIX + id;
        for (InWorldNode n : sd.getNodes()) {
            InWorldNodeData d = sd.getNodeData(n);
            if (d != null && label.equals(d.label)) return n;
        }
        return null;
    }

    private static String sectionLinkKey(BlockPos pos, int index, InWorldNode shadow) {
        return pos.asLong() + ":" + index + ":" + shadow.id();
    }

    private static void removeStaleSectionCompatibilityLinks(ServerLevel level,
                                                              InfrastructureSavedData sd,
                                                              Set<String> expected) {
        for (OheSectionBlockEntity section : SectionRegistry.get(level)) {
            BlockPos pos = section.getBlockPos();
            for (int index = 0; index < 2; index++) {
                InWorldNode cee = new InWorldNode(index, pos);
                if (!sd.hasNode(cee)) continue;
                for (var connection : List.copyOf(sd.getConnections(cee))) {
                    InWorldNode other = connection.node1().equals(cee) ? connection.node2() : connection.node1();
                    if (!isPawNode(other, sd)) continue;
                    if (!expected.contains(sectionLinkKey(pos, index, other))) {
                        sd.removeConnectionNoDrops(connection);
                    }
                }
            }
        }
    }

    private static Set<String> syncSwitchCompatibilityLinks(ServerLevel level,
                                                             InfrastructureSavedData sd,
                                                             WireGraph graph) {
        Set<String> expected = new HashSet<>();
        for (WireNode pn : graph.getNodes()) {
            InWorldNode shadow = nodesFor(sd, pn.getId());
            if (shadow == null) continue;
            org.joml.Vector3d endpoint = attachmentPosition(pn, null);
            BlockPos base = BlockPos.containing(endpoint.x, endpoint.y, endpoint.z);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos assemblyPos = base.offset(dx, dy, dz);
                        BlockState assemblyState = level.getBlockState(assemblyPos);
                        if (!(assemblyState.getBlock() instanceof OheSwitchAssemblyBlock)) continue;
                        if (assemblyState.getValue(OheSwitchAssemblyBlock.FEEDER_MODE)) continue;
                        BlockPos switchPos = findExactHvSwitchNode(level, endpoint, assemblyPos);
                        if (switchPos == null) continue;
                        BlockState switchState = level.getBlockState(switchPos);
                        if (!(switchState.getBlock() instanceof HVSwitchBlock hv)) continue;
                        sd.registerOrUpdateNodes(switchPos, List.of(0, 1));
                        for (int nodeIndex = 0; nodeIndex < 2; nodeIndex++) {
                            Vec3 np = hv.getNodePosition(level, switchPos, switchState, nodeIndex);
                            org.joml.Vector3d world = new org.joml.Vector3d(switchPos.getX()+np.x, switchPos.getY()+np.y, switchPos.getZ()+np.z);
                            if (endpoint.distance(world) > 0.08D) continue;
                            InWorldNode cee = new InWorldNode(nodeIndex, switchPos);
                            if (!sd.hasNode(cee)) continue;
                            sd.createNode(cee, new Vec3(world.x, world.y, world.z), Vec3.ZERO);
                            expected.add(switchLinkKey(switchPos, nodeIndex, shadow));
                            ensureConnected(sd, shadow, cee, Math.max(0.01, endpoint.distance(world)));
                        }
                    }
                }
            }
        }
        return expected;
    }

    private static BlockPos findExactHvSwitchNode(ServerLevel level, org.joml.Vector3d endpoint, BlockPos assemblyPos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = assemblyPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(p);
                    if (!(state.getBlock() instanceof HVSwitchBlock hv)) continue;
                    for (int i = 0; i < 2; i++) {
                        Vec3 np = hv.getNodePosition(level, p, state, i);
                        org.joml.Vector3d world = new org.joml.Vector3d(p.getX()+np.x, p.getY()+np.y, p.getZ()+np.z);
                        if (endpoint.distance(world) <= 0.08D) return p;
                    }
                }
            }
        }
        return null;
    }

    private static String switchLinkKey(BlockPos pos, int index, InWorldNode shadow) {
        return pos.asLong() + ":" + index + ":" + shadow.id();
    }

    private static void removeStaleSwitchCompatibilityLinks(ServerLevel level,
                                                             InfrastructureSavedData sd,
                                                             Set<String> expected) {
        for (InWorldNode node : List.copyOf(sd.getNodes())) {
            InWorldNodeData data = sd.getNodeData(node);
            if (data == null || data.label == null || !data.label.startsWith(NODE_PREFIX)) continue;
            for (var connection : List.copyOf(sd.getConnections(node))) {
                InWorldNode other = connection.node1().equals(node) ? connection.node2() : connection.node1();
                if (other.id() < 0) continue;
                BlockPos source = other.sourcePos();
                if (!level.isLoaded(source)) continue;
                if (!(level.getBlockState(source).getBlock() instanceof HVSwitchBlock)) continue;
                int index = other.id();
                if (!expected.contains(switchLinkKey(source, index, node))) {
                    sd.removeConnectionNoDrops(connection);
                }
            }
        }
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

    private static Set<String> syncFeederBridgeLinks(ServerLevel level, InfrastructureSavedData sd,
                                                       WireGraph graph, Map<UUID, InWorldNode> nodes) {
        Set<String> expected = new HashSet<>();
        for (WireNode pn : graph.getNodes()) {
            if (!hasFeederEdge(graph, pn.getId())) continue;
            InWorldNode shadow = nodes.get(pn.getId());
            if (shadow == null) continue;
            org.joml.Vector3d endpoint = attachmentPosition(pn, null);
            BlockPos feederPos = findFeederBridgeAtEndpoint(level, endpoint);
            if (feederPos == null) continue;
            sd.registerOrUpdateNodes(feederPos, List.of(0));
            InWorldNode ceeNode = new InWorldNode(0, feederPos);
            if (!sd.hasNode(ceeNode)) continue;
            expected.add(bridgeLinkKey(shadow, feederPos));
            org.joml.Vector3d p = attachmentPosition(pn, null);
            Vec3 cp = sd.getNodePositionOrCenter(ceeNode);
            ensureConnected(sd, shadow, ceeNode,
                    Math.max(0.01D, p.distance(cp.x, cp.y, cp.z)));
        }
        return expected;
    }

    private static BlockPos findFeederBridgeAtEndpoint(ServerLevel level, org.joml.Vector3d endpoint) {
        BlockPos base = BlockPos.containing(endpoint.x, endpoint.y, endpoint.z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = base.offset(dx, dy, dz);
                    if (!level.isLoaded(candidate)) continue;
                    BlockState state = level.getBlockState(candidate);
                    if (!(state.getBlock() instanceof com.gokul.ohecompat.block.OheFeederBridgeBlock bridge)) continue;
                    Vec3 local = bridge.getConnectorPoint(level, candidate, state);
                    org.joml.Vector3d expected = new org.joml.Vector3d(
                            candidate.getX()+local.x, candidate.getY()+local.y, candidate.getZ()+local.z);
                    if (endpoint.distance(expected) <= 0.08D) return candidate;
                }
            }
        }
        return null;
    }

    private static void removeStaleFeederBridgeLinks(ServerLevel level, InfrastructureSavedData sd,
                                                      Set<String> expected) {
        for (InWorldNode shadow : List.copyOf(sd.getNodes())) {
            InWorldNodeData data = sd.getNodeData(shadow);
            if (data == null || data.label == null || !data.label.startsWith(FEEDER_NODE_PREFIX)) continue;
            for (var connection : List.copyOf(sd.getConnections(shadow))) {
                InWorldNode other = connection.node1().equals(shadow) ? connection.node2() : connection.node1();
                if (other.id() != 0) continue;
                BlockPos pos = other.sourcePos();
                if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock()
                        instanceof com.gokul.ohecompat.block.OheFeederBridgeBlock)) continue;
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
                    return e.getType().getRegistryId() + ":" + e.getId() + ":" + e.getNodeAId() + ":" + e.getNodeBId() +
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
