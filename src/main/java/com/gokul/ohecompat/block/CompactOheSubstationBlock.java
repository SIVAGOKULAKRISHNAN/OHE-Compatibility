package com.gokul.ohecompat.block;

import com.gokul.ohecompat.registry.ModBlocks;
import com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlock;
import com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CompactOheSubstationBlock extends TransformerBlock {
    public CompactOheSubstationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<TransformerBlockEntity> getBlockEntityClass() {
        return TransformerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TransformerBlockEntity> getBlockEntityType() {
        return ModBlocks.COMPACT_SUBSTATION_BE.get();
    }
}
