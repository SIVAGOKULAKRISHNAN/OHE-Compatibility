package com.gokul.ohecompat.integration;

import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.wires.graph.NewWireCollision;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.GraphId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.Optional;
import java.util.UUID;

/**
 * Immutable, exact P&W wire selection.
 *
 * The selection identifies the real P&W graph/edge and the exact longitudinal
 * point on one of that edge's native wire components. No nearest-node lookup
 * or guessed offset is involved.
 */
public record PawWireSelection(
        String graphId,
        UUID edgeId,
        String wireName,
        double percentage,
        int channel) {

    public PawWireSelection {
        graphId = graphId == null ? "" : graphId;
        wireName = wireName == null ? "" : wireName;
        percentage = Math.max(0.0D, Math.min(1.0D, percentage));
    }

    public static Optional<PawWireSelection> fromHit(Level level, WireHitResult hit) {
        if (level == null || hit == null) return Optional.empty();

        try {
            WireGraph graph = WireGraphManager.get(level, hit.getGraphId());
            WireEdge edge = graph.getEdge(hit.getWireId().id());
            if (edge == null) return Optional.empty();

            NewWireCollision collision = graph.getCollisionById(edge.getId()).orElse(null);
            if (collision == null || !collision.hasWire(hit.getWireId().name())) return Optional.empty();

            String wireName = hit.getWireId().name();
            double length = collision.length(wireName);
            if (length <= 1.0E-6D) return Optional.empty();

            double pct = hit.getPosOnWire() / length;
            return Optional.of(new PawWireSelection(
                    hit.getGraphId().id(),
                    edge.getId(),
                    wireName,
                    pct,
                    0));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Optional<Vector3d> exactPosition(ServerLevel level) {
        try {
            WireGraph graph = WireGraphManager.get(level, new GraphId(graphId));
            WireEdge edge = graph.getEdge(edgeId);
            if (edge == null) return Optional.empty();

            NewWireCollision collision = graph.getCollisionById(edge.getId()).orElse(null);
            if (collision == null || !collision.hasWire(wireName)) return Optional.empty();

            double distance = percentage * collision.length(wireName);
            return Optional.of(collision.wirePosToWorldPos(wireName, distance));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Optional<WireEdge> edge(ServerLevel level) {
        try {
            WireGraph graph = WireGraphManager.get(level, new GraphId(graphId));
            return Optional.ofNullable(graph.getEdge(edgeId));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public boolean isPawCatenary() {
        return "pantographsandwires:catenary_wire".equals(
                edgeTypeIdHint());
    }

    public boolean isNamedWire(String name) {
        return name != null && name.equals(wireName);
    }

    /**
     * Stable string for save/sync signatures.
     */
    public String stableKey() {
        return graphId + ":" + edgeId + ":" + wireName + ":" +
                Double.toString(percentage) + ":" + channel;
    }

    private String edgeTypeIdHint() {
        return "";
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("GraphId", graphId);
        tag.putUUID("EdgeId", edgeId);
        tag.putString("WireName", wireName);
        tag.putDouble("Percentage", percentage);
        tag.putInt("Channel", channel);
        return tag;
    }

    public static Optional<PawWireSelection> fromNbt(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("EdgeId") || !tag.contains("GraphId") || !tag.contains("WireName")) {
            return Optional.empty();
        }
        return Optional.of(new PawWireSelection(
                tag.getString("GraphId"),
                tag.getUUID("EdgeId"),
                tag.getString("WireName"),
                tag.contains("Percentage") ? tag.getDouble("Percentage") : 0.5D,
                tag.contains("Channel") ? tag.getInt("Channel") : 0));
    }
}
