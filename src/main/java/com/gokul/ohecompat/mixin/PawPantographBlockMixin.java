package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.CEEPantographTypes;
import com.george_vi.electroenergetics.content.railway_electrification.pantograph.IPantographBlock;
import com.george_vi.electroenergetics.content.railway_electrification.pantograph.PantographType;
import de.mrjulsen.paw.block.PantographBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PantographBlock.class)
public abstract class PawPantographBlockMixin implements IPantographBlock {
    @Override
    public PantographType getPantographType(BlockState state) {
        return CEEPantographTypes.STANDARD.get();
    }

    @Override
    public boolean isSidewaysPantograph() {
        return false;
    }
}
