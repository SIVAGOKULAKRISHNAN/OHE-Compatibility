package com.gokul.ohecompat.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;

/**
 * Native P&W connector block entity used by OHE compatibility components.
 * P&W's wire items require the target block entity to be a WireConnectorBlockEntity
 * in addition to the block implementing IWireConnector.
 */
public class PawConnectorBlockEntity extends WireConnectorBlockEntity {
    public PawConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
