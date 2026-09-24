package com.gokul.ohecompat.blockentity;

import com.gokul.ohecompat.integration.FeederBridgeRegistry;
import com.gokul.ohecompat.integration.PawWireSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class OheFeederBridgeBlockEntity extends PawConnectorBlockEntity {
    private PawWireSelection energyWire;
    private PawWireSelection oheWire;

    public OheFeederBridgeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PawWireSelection energyWire() {
        return energyWire;
    }

    public PawWireSelection oheWire() {
        return oheWire;
    }

    public boolean linked() {
        return energyWire != null && oheWire != null;
    }

    public void setSelections(PawWireSelection energyWire, PawWireSelection oheWire) {
        this.energyWire = energyWire;
        this.oheWire = oheWire;
        setChanged();
    }

    public void clearSelections() {
        energyWire = null;
        oheWire = null;
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            FeederBridgeRegistry.register(server, this);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            FeederBridgeRegistry.unregister(server, getBlockPos());
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (energyWire != null) tag.put("EnergyWire", energyWire.toNbt());
        if (oheWire != null) tag.put("OheWire", oheWire.toNbt());
    }

    @Override
    protected void loadAdditional(CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyWire = tag.contains("EnergyWire")
                ? PawWireSelection.fromNbt(tag.getCompound("EnergyWire")).orElse(null)
                : null;
        oheWire = tag.contains("OheWire")
                ? PawWireSelection.fromNbt(tag.getCompound("OheWire")).orElse(null)
                : null;
    }
}
