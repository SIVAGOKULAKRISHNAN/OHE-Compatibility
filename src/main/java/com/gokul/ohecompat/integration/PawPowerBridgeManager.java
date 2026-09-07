package com.gokul.ohecompat.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Legacy compatibility shim.
 *
 * Older builds used this class to find the nearest P&W shadow node to a bridge
 * and create a CEE shadow connection. That proximity-based behaviour could
 * attach an unrelated P&W line to the bridge. Native P&W graph synchronization
 * now performs exact endpoint matching in PawShadowCatenaryManager instead.
 */
@Deprecated
public final class PawPowerBridgeManager {
    private PawPowerBridgeManager() {}

    public static void syncBridge(ServerLevel level, BlockPos pos, BlockState state) {
        // Intentionally no-op. Do not restore nearest-node discovery here.
    }
}
