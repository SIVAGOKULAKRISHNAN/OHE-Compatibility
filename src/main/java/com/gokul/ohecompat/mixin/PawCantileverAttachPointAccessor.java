package com.gokul.ohecompat.mixin;

import de.mrjulsen.paw.block.abstractions.AbstractRotatableWireConnectorBlock;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes P&W's native protected attachment-point calculation to the
 * compatibility adapter without duplicating or hard-coding the cantilever
 * geometry. The implementation remains entirely in P&W.
 */
@Mixin(AbstractRotatableWireConnectorBlock.class)
public interface PawCantileverAttachPointAccessor {
    @Invoker("defaultWireAttachPoint")
    Vec3 ohecompat$defaultWireAttachPoint(Level level, BlockPos pos, BlockState state,
                                           CustomData data, int index);
}
