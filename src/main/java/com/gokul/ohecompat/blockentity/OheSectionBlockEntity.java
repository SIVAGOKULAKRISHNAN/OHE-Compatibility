package com.gokul.ohecompat.blockentity;

import com.gokul.ohecompat.block.OheSectionAssemblyBlock;
import com.gokul.ohecompat.integration.SectionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class OheSectionBlockEntity extends WireConnectorBlockEntity {
    private UUID edgeId;
    private String wireName;
    private double percentage = 0.5;

    public OheSectionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public UUID edgeId() { return edgeId; }
    public String wireName() { return wireName; }
    public double percentage() { return percentage; }

    public void setLink(UUID edgeId, String wireName, double percentage) {
        this.edgeId = edgeId;
        this.wireName = wireName;
        this.percentage = Math.max(0.02, Math.min(0.98, percentage));
        setChanged();
    }

    public void clearLink() {
        this.edgeId = null;
        this.wireName = null;
        this.percentage = 0.5;
        setChanged();
    }

    public boolean isolated() {
        return getBlockState().getValue(OheSectionAssemblyBlock.ISOLATED);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            SectionRegistry.register(server, this);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            SectionRegistry.unregister(server, getBlockPos());
        }
        super.setRemoved();
    }

    public void tick() {
        // Kept intentionally empty: section topology is dirty-driven by the registry.
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (edgeId != null) tag.putUUID("EdgeId", edgeId);
        if (wireName != null) tag.putString("WireName", wireName);
        tag.putDouble("Percentage", percentage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        edgeId = tag.hasUUID("EdgeId") ? tag.getUUID("EdgeId") : null;
        wireName = tag.contains("WireName") ? tag.getString("WireName") : null;
        percentage = tag.contains("Percentage") ? tag.getDouble("Percentage") : 0.5;
    }
}
