package com.gokul.ohecompat.integration;

import com.gokul.ohecompat.blockentity.OheJunctionLineBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.*;

public final class JunctionRegistry {
    private static final Map<ServerLevel, Map<BlockPos, OheJunctionLineBlockEntity>> ENTRIES = new WeakHashMap<>();

    public static void register(ServerLevel level, OheJunctionLineBlockEntity be) {
        ENTRIES.computeIfAbsent(level, x -> new HashMap<>()).put(be.getBlockPos(), be);
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        Map<BlockPos, OheJunctionLineBlockEntity> map = ENTRIES.get(level);
        if (map == null) return;
        map.remove(pos);
        if (map.isEmpty()) ENTRIES.remove(level);
    }

    public static Collection<OheJunctionLineBlockEntity> get(ServerLevel level) {
        return List.copyOf(ENTRIES.getOrDefault(level, Map.of()).values());
    }

    public static String signature(ServerLevel level) {
        return get(level).stream()
                .sorted(Comparator.comparing(be -> be.getBlockPos().asLong()))
                .map(be -> be.getBlockPos().asLong() + ":" +
                        (be.selectionA() == null ? "-" : be.selectionA().stableKey()) + ":" +
                        (be.selectionB() == null ? "-" : be.selectionB().stableKey()))
                .reduce("", String::concat);
    }

    private JunctionRegistry() {}
}
