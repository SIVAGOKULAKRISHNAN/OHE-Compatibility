package com.gokul.ohecompat.blockentity;

import com.gokul.ohecompat.integration.JunctionRegistry;
import com.gokul.ohecompat.integration.PawWireSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Stores the two explicit P&W OHE selections for one Junction Line.
 */
public final class OheJunctionLineBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    private PawWireSelection a;
    private PawWireSelection b;

    public OheJunctionLineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PawWireSelection selectionA() {
        return a;
    }

    public PawWireSelection selectionB() {
        return b;
    }

    public boolean linked() {
        return a != null && b != null;
    }

    public void setSelections(PawWireSelection a, PawWireSelection b) {
        this.a = a;
        this.b = b;
        setChanged();
    }

    public void clearSelections() {
        this.a = null;
        this.b = null;
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            JunctionRegistry.register(server, this);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            JunctionRegistry.unregister(server, getBlockPos());
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (a != null) tag.put("SelectionA", a.toNbt());
        if (b != null) tag.put("SelectionB", b.toNbt());
    }

    @Override
    protected void loadAdditional(CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        a = tag.contains("SelectionA")
                ? PawWireSelection.fromNbt(tag.getCompound("SelectionA")).orElse(null)
                : null;
        b = tag.contains("SelectionB")
                ? PawWireSelection.fromNbt(tag.getCompound("SelectionB")).orElse(null)
                : null;
    }
}
