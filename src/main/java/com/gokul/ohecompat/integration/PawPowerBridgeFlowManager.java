package com.gokul.ohecompat.integration;

import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.george_vi.electroenergetics.simulation.infrastructure.InWorldNodeData;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeType;
import com.gokul.ohecompat.config.OheConfig;
import com.gokul.ohecompat.block.OhePowerBridgeBlock;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * Final P&W -> CEE hand-off for the OHE Power Bridge.
 *
 * The P&W wire graph remains authoritative for the physical connection. This
 * listener only mirrors the exact connected P&W bridge endpoint into CEE after
 * the normal shadow manager has finished its cleanup pass. This is deliberately
 * LOWEST priority so an old/stale bridge link cannot survive the same tick.
 */
@EventBusSubscriber(modid = "ohecompat")
public final class PawPowerBridgeFlowManager {
    private static final String NODE_PREFIX = "ohecompat:paw:ohe:";
    private static final double ENDPOINT_EPSILON = 0.08D;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(ServerTickEvent.Post event) {
        if (!OheConfig.ENABLE_SHADOW_CEE_NETWORK.get() || !PawBridge.present()) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            sync(level);
        }
    }

    private static void sync(ServerLevel level) {
        WireGraph graph = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);
        if (graph == null) return;

        InfrastructureSavedData sd = InfrastructureSavedData.load(level);

        for (WireNode pn : graph.getNodes()) {
            org.joml.Vector3d nodePosition = pn.getPos();
            BlockPos bridgePos = new BlockPos(
                    (int) Math.floor(nodePosition.x),
                    (int) Math.floor(nodePosition.y),
                    (int) Math.floor(nodePosition.z));
            BlockState bridgeState = level.getBlockState(bridgePos);
            if (!(bridgeState.getBlock() instanceof OhePowerBridgeBlock)) continue;
            if (!hasCatenaryEdge(graph, pn.getId())) continue;

            InWorldNode shadow = getOrCreateShadow(sd, pn);
            if (shadow == null) continue;

            // Use the real P&W wire attachment point from the edge, not the
            // connector block centre. This is the point where power enters the
            // P&W catenary graph.
            Vec3 attachment = null;
            for (WireEdge edge : graph.getEdges()) {
                if (!isCatenary(edge)) continue;
                ConnectorDataProvider provider = null;
                if (edge.getNodeAId().equals(pn.getId())) {
                    provider = edge.getWireConnectionData().connectorA();
                } else if (edge.getNodeBId().equals(pn.getId())) {
                    provider = edge.getWireConnectionData().connectorB();
                }
                if (provider == null) continue;
                org.joml.Vector3d p = attachmentPosition(pn, provider);
                attachment = new Vec3(p.x, p.y, p.z);
                break;
            }
            if (attachment == null) continue;

            sd.createNode(shadow, attachment, Vec3.ZERO);

            BlockPos ceeBlockPos = new BlockPos(
                    (int) Math.floor(nodePosition.x),
                    (int) Math.floor(nodePosition.y),
                    (int) Math.floor(nodePosition.z));
            sd.registerOrUpdateNodes(ceeBlockPos, List.of(0));
            InWorldNode ceeNode = new InWorldNode(0, ceeBlockPos);
            if (!sd.hasNode(ceeNode)) continue;

            Vec3 ceePosition = sd.getNodePositionOrCenter(ceeNode);
            double length = Math.max(0.01D, attachment.distanceTo(ceePosition));
            ensureConnected(sd, shadow, ceeNode, length);
        }
    }

    private static InWorldNode getOrCreateShadow(InfrastructureSavedData sd, WireNode pn) {
        String label = NODE_PREFIX + pn.getId();
        for (InWorldNode node : sd.getNodes()) {
            InWorldNodeData data = sd.getNodeData(node);
            if (data != null && label.equals(data.label)) return node;
        }

        org.joml.Vector3d p = new org.joml.Vector3d(pn.getPos());
        InWorldNodeData data = sd.createDetachedNode(DetachedNodeType.FIXED,
                new Vec3(p.x, p.y, p.z));
        data.label = label;
        return data.node;
    }

    private static boolean hasCatenaryEdge(WireGraph graph, UUID nodeId) {
        for (WireEdge edge : graph.getEdges()) {
            if (isCatenary(edge) &&
                    (edge.getNodeAId().equals(nodeId) || edge.getNodeBId().equals(nodeId))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCatenary(WireEdge edge) {
        return edge != null && "pantographsandwires:catenary_wire".equals(
                edge.getType().getRegistryId().toString());
    }

    private static org.joml.Vector3d attachmentPosition(WireNode node, ConnectorDataProvider provider) {
        org.joml.Vector3d p = new org.joml.Vector3d(node.getPos());
        if (provider instanceof BasicConnectorDataProvider basic) {
            p.add(basic.getAttachOffset());
        }
        return p;
    }

    private static void ensureConnected(InfrastructureSavedData sd, InWorldNode a,
                                        InWorldNode b, double length) {
        for (var connection : sd.getConnections(a)) {
            boolean matches = (connection.node1().equals(a) && connection.node2().equals(b))
                    || (connection.node1().equals(b) && connection.node2().equals(a));
            if (!matches) continue;

            WireData existing = sd.getConnectionData(connection);
            if (existing == null || existing.wireType() != PawShadowWireType.SHADOW.get()) {
                sd.setConnectionData(connection,
                        WireData.ofLength(PawShadowWireType.SHADOW.get(), Math.max(0.01D, length)));
            }
            return;
        }

        sd.connect(a, b, WireData.ofLength(PawShadowWireType.SHADOW.get(), Math.max(0.01D, length)));
    }

    private PawPowerBridgeFlowManager() {}
}
