package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.CEEDataComponents;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.simulation.WireType;
import com.george_vi.electroenergetics.simulation.infrastructure.InfrastructureSavedData;
import com.gokul.ohecompat.integration.PawCeeEndpointManager;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends CEE's native wire-spool endpoint discovery to native P&W cantilevers.
 *
 * This does not turn a P&W cantilever into a CEE electrical block and does not
 * create a second visible wire. It creates one CEE detached electrical node at
 * P&W's own default catenary attachment point and lets CEE's existing wire
 * simulation/rendering use that endpoint.
 */
@Mixin(targets = "com.george_vi.electroenergetics.content.wire_spool.WireSpoolItem")
public abstract class CeeWireSpoolPawCantileverMixin {
    @Shadow @Final
    private java.util.function.Supplier<WireType> wireType;
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, require = 1)
    private void ohecompat$allowPawCantilever(UseOnContext context,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractCantileverBlock)) return;

        ItemStack stack = context.getItemInHand();

        // Client side: accept the interaction immediately. The server creates
        // and synchronizes the selected node through the normal item stack data
        // component used by CEE's wire spool.
        if (!(level instanceof ServerLevel server)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        InfrastructureSavedData sd = InfrastructureSavedData.load(server);
        InWorldNode endpoint = PawCeeEndpointManager.getOrCreate(server, pos, state);
        if (endpoint == null) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        InWorldNode selected = stack.get(CEEDataComponents.SELECTED_NODE);
        if (selected == null) {
            stack.set(CEEDataComponents.SELECTED_NODE, endpoint);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (selected.equals(endpoint) || sd.isConnected(selected, endpoint)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        WireType wireType = getWireType(context);
        if (wireType == null) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if (selected.getPosition(server) == null || endpoint.getPosition(server) == null) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        double length = selected.getPosition(server).distanceTo(endpoint.getPosition(server));
        if (length > wireType.getMaxLength()) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        sd.connect(selected, endpoint, wireType);
        stack.remove(CEEDataComponents.SELECTED_NODE);
        if (!context.getPlayer().isCreative()) stack.shrink(1);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    private WireType getWireType(UseOnContext context) {
        return wireType == null ? null : wireType.get();
    }
}
