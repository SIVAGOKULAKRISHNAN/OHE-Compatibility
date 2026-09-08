package com.gokul.ohecompat.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P&W WireConnectorBlockEntity for the universal TSS feeder isolator.
 * Keeps a lightweight registry of loaded TSS positions so the compatibility
 * manager can apply the CEE shadow switching logic without scanning the world.
 */
public final class TssFeederSwitchBlockEntity extends PawConnectorBlockEntity {
    private static final Map<ServerLevel, Set<BlockPos>> LOADED = new WeakHashMap<>();

    public TssFeederSwitchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerLoaded();
    }

    @Override
    public void onChunkUnloaded() {
        unregisterLoaded();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        unregisterLoaded();
        super.setRemoved();
    }

    private void registerLoaded() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel server)) return;
        synchronized (LOADED) {
            LOADED.computeIfAbsent(server, ignored -> ConcurrentHashMap.newKeySet()).add(getBlockPos());
        }
    }

    private void unregisterLoaded() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel server)) return;
        synchronized (LOADED) {
            Set<BlockPos> positions = LOADED.get(server);
            if (positions != null) {
                positions.remove(getBlockPos());
                if (positions.isEmpty()) LOADED.remove(server);
            }
        }
    }

    public static Set<BlockPos> getLoaded(ServerLevel level) {
        synchronized (LOADED) {
            Set<BlockPos> positions = LOADED.get(level);
            if (positions == null) return Set.of();
            return Collections.unmodifiableSet(Set.copyOf(positions));
        }
    }
}
