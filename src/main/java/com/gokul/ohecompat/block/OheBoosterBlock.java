package com.gokul.ohecompat.block;

import com.gokul.ohecompat.registry.ModBlocks;
import com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlock;
import com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class OheBoosterBlock extends TransformerBlock {
    public OheBoosterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<TransformerBlockEntity> getBlockEntityClass() {
        return TransformerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TransformerBlockEntity> getBlockEntityType() {
        return ModBlocks.OHE_BOOSTER_BE.get();
    }
}
