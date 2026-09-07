package com.gokul.ohecompat.integration;

import com.george_vi.electroenergetics.CEEBlocks;
import com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchBlockEntity;
import com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchDevice;
import com.george_vi.electroenergetics.devices.device.DevicesSavedData;
import com.simibubi.create.AllItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CeeSwitchAdapter {
    private CeeSwitchAdapter() {}

    public static boolean isWrench(ItemStack stack) {
        return AllItems.WRENCH.isIn(stack);
    }

    public static boolean toggleExistingSwitch(Level level, BlockPos assembly) {
        if (!(level instanceof ServerLevel server))
            return false;

        // The railway assembly does not own an electrical device. It looks for
        // an already-placed native CEE HV Switch on/near the mounting position.
        for (Direction dir : Direction.values()) {
            for (int distance = 0; distance <= 2; distance++) {
                BlockPos p = assembly.relative(dir, distance);
                if (!CEEBlocks.HV_SWITCH.has(level.getBlockState(p)))
                    continue;

                BlockEntity be = level.getBlockEntity(p);
                if (!(be instanceof HVSwitchBlockEntity hvBe))
                    continue;

                // Reuse the same public CEE state transition used by the native
                // HV-switch block.  CEE remains the owner of electrical behavior;
                // this adapter only forwards the wrench action to the existing device.
                DevicesSavedData devices = DevicesSavedData.load(server);
                HVSwitchDevice device = devices.getDevice(p, HVSwitchDevice.class);
                if (device == null) return false;
                device.isConnecting = !hvBe.connected;
                hvBe.connected = !hvBe.connected;
                hvBe.sendData();
                return true;
            }
        }
        return false;
    }
}
