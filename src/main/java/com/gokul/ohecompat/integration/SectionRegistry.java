package com.gokul.ohecompat.integration;

import com.gokul.ohecompat.blockentity.OheSectionBlockEntity;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.NewWireCollision;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public final class SectionRegistry {
    private static final Map<ServerLevel, Map<BlockPos, OheSectionBlockEntity>> SECTIONS = new WeakHashMap<>();

    public static void register(ServerLevel level, OheSectionBlockEntity be) {
        SECTIONS.computeIfAbsent(level, l -> new HashMap<>()).put(be.getBlockPos(), be);
        if (be.edgeId() == null) capture(level, be);
    }

    public static Collection<OheSectionBlockEntity> get(ServerLevel level) {
        return List.copyOf(SECTIONS.getOrDefault(level, Map.of()).values());
    }

    public static boolean refreshUnlinked(ServerLevel level) {
        boolean changed = false;
        for (OheSectionBlockEntity be : get(level)) {
            if (be.edgeId() == null || be.wireName() == null) {
                UUID before = be.edgeId();
                capture(level, be);
                changed |= before == null && be.edgeId() != null;
            } else {
                WireGraph graph = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);
                if (graph == null || graph.getEdge(be.edgeId()) == null) {
                    be.clearLink();
                    capture(level, be);
                    changed = true;
                }
            }
        }
        return changed;
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        Map<BlockPos, OheSectionBlockEntity> map = SECTIONS.get(level);
        if (map == null) return;
        map.remove(pos);
        if (map.isEmpty()) SECTIONS.remove(level);
    }

    public static String signature(ServerLevel level) {
        return get(level).stream()
                .sorted(Comparator.comparing(be -> be.getBlockPos().asLong()))
                .map(be -> be.getBlockPos() + ":" + be.edgeId() + ":" + be.percentage() + ":" + be.isolated())
                .reduce("", String::concat);
    }

    private static void capture(ServerLevel level, OheSectionBlockEntity be) {
        WireGraph graph = WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES);
        if (graph == null) return;
        Collection<NewWireCollision> collisions = graph.getCollisionsInBlock(be.getBlockPos());
        NewWireCollision best = null;
        String bestWire = null;
        double bestPct = 0.5;
        double bestDist = Double.MAX_VALUE;
        for (NewWireCollision collision : collisions) {
            for (NewWireCollision.WireBlockCollision c : collision.collisionsInBlock(be.getBlockPos())) {
                String name = c.getWireName();
                double pct = Math.max(0.02, Math.min(0.98,
                        collision.worldPosToWirePos(name,
                                new org.joml.Vector3d(be.getBlockPos().getX()+0.5,
                                        be.getBlockPos().getY()+0.5,
                                        be.getBlockPos().getZ()+0.5))));
                org.joml.Vector3d p = collision.wirePosToWorldPos(name, pct);
                double d = p.distance(be.getBlockPos().getCenter().x, be.getBlockPos().getCenter().y, be.getBlockPos().getCenter().z);
                if (d < bestDist) {
                    bestDist = d;
                    best = collision;
                    bestWire = name;
                    bestPct = pct;
                }
            }
        }
        if (best != null && graph.getEdge(best.getId()) != null) {
            be.setLink(best.getId(), bestWire, bestPct);
        }
    }

    private SectionRegistry() {}
}
