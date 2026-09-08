package com.gokul.ohecompat.integration;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

/** Placement/mounting rules for compatibility equipment that belongs on the P&W OHE. */
public final class PawOhePlacement {
    private static final double MAX_WIRE_DISTANCE = 0.85D;

    public static boolean onCatenary(Level level, BlockPos pos) {
        return nearWire(level, pos, "pantographsandwires:catenary_wire");
    }

    public static boolean onEnergyWire(Level level, BlockPos pos) {
        return nearWire(level, pos, "pantographsandwires:energy_wire");
    }

    /**
     * Returns true only when the requested block position is physically on/very
     * close to an existing P&W wire segment. This is a placement rule, not an
     * electrical proximity link: no CEE node is ever connected here.
     */
    private static boolean nearWire(Level level, BlockPos pos, String registryId) {
        if (!(level instanceof ServerLevel server)) return true;
        WireGraph graph = WireGraphManager.get(server, WiresApi.PAW_CATENARY_WIRES);
        if (graph == null) return false;

        Vector3d target = new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        for (WireEdge edge : graph.getEdges()) {
            if (edge == null || !registryId.equals(edge.getType().getRegistryId().toString())) continue;

            WireNode a = findNode(graph, edge.getNodeAId());
            WireNode b = findNode(graph, edge.getNodeBId());
            if (a == null || b == null) continue;

            Vector3d pa = endpoint(a, edge.getWireConnectionData().connectorA());
            Vector3d pb = endpoint(b, edge.getWireConnectionData().connectorB());
            if (distancePointToSegment(target, pa, pb) <= MAX_WIRE_DISTANCE) return true;
        }
        return false;
    }

    private static WireNode findNode(WireGraph graph, java.util.UUID id) {
        for (WireNode node : graph.getNodes()) {
            if (id.equals(node.getId())) return node;
        }
        return null;
    }

    private static Vector3d endpoint(WireNode node, ConnectorDataProvider provider) {
        Vector3d p = new Vector3d(node.getPos());
        if (provider instanceof BasicConnectorDataProvider basic) p.add(basic.getAttachOffset());
        return p;
    }

    private static double distancePointToSegment(Vector3d p, Vector3d a, Vector3d b) {
        Vector3d ab = new Vector3d(b).sub(a);
        double len2 = ab.lengthSquared();
        if (len2 <= 1.0E-8D) return p.distance(a);
        double t = new Vector3d(p).sub(a).dot(ab) / len2;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vector3d closest = new Vector3d(a).fma(t, ab);
        return p.distance(closest);
    }

    /** P&W mast/pole support used by the D switch. */
    public static boolean onPawPole(Level level, BlockPos supportPos) {
        BlockState state = level.getBlockState(supportPos);
        Block block = state.getBlock();
        var id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !"pantographsandwires".equals(id.getNamespace())) return false;
        String path = id.getPath();
        return path.contains("mast") || path.equals("concrete_post") || path.contains("cantilever_bracket");
    }

    private PawOhePlacement() {}
}
