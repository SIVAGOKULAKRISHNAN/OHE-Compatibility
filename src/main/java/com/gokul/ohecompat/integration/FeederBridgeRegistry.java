package com.gokul.ohecompat.integration;

import com.gokul.ohecompat.blockentity.OheFeederBridgeBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.*;

public final class FeederBridgeRegistry {
    private static final Map<ServerLevel, Map<BlockPos, OheFeederBridgeBlockEntity>> ENTRIES = new WeakHashMap<>();

    public static void register(ServerLevel level, OheFeederBridgeBlockEntity be) {
        ENTRIES.computeIfAbsent(level, x -> new HashMap<>()).put(be.getBlockPos(), be);
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        Map<BlockPos, OheFeederBridgeBlockEntity> map = ENTRIES.get(level);
        if (map == null) return;
        map.remove(pos);
        if (map.isEmpty()) ENTRIES.remove(level);
    }

    public static Collection<OheFeederBridgeBlockEntity> get(ServerLevel level) {
        return List.copyOf(ENTRIES.getOrDefault(level, Map.of()).values());
    }

    public static String signature(ServerLevel level) {
        return get(level).stream()
                .sorted(Comparator.comparing(be -> be.getBlockPos().asLong()))
                .map(be -> be.getBlockPos().asLong() + ":" +
                        (be.energyWire() == null ? "-" : be.energyWire().stableKey()) + ":" +
                        (be.oheWire() == null ? "-" : be.oheWire().stableKey()))
                .reduce("", String::concat);
    }

    private FeederBridgeRegistry() {}
}
