package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryHolderBlock;
import com.gokul.ohecompat.block.OhePowerBridgeBlock;
import com.gokul.ohecompat.block.OheSectionAssemblyBlock;
import com.gokul.ohecompat.block.OheSwitchAssemblyBlock;
import com.gokul.ohecompat.registry.ModBlocks;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.paw.item.CatenaryWireItem;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes compatibility OHE connectors use P&W's native BlockConnectorNodeData
 * path. P&W remains authoritative for the physical catenary graph.
 */
@Mixin(CatenaryWireItem.class)
public abstract class PawCatenaryWireItemMixin {
    @Inject(method = "createNodeData", at = @At("HEAD"), cancellable = true)
    private void ohecompat$allowNativeEndpoints(Level level, Player player, InteractionHand hand,
                                                  HitResult hitResult,
                                                  CallbackInfoReturnable<NodeData> cir) {
        BlockPos hitPos = null;
        if (hitResult instanceof BlockHitResult blockHit) hitPos = blockHit.getBlockPos();
        else if (hitResult instanceof de.mrjulsen.paw.util.collision.RaycastHitResult rayHit)
            hitPos = rayHit.getBlockPos();
        if (hitPos == null) return;

        BlockPos connectorPos = resolveConnector(level, hitPos);
        if (connectorPos == null) return;

        BlockState state = level.getBlockState(connectorPos);
        if (!(state.getBlock() instanceof ICatenaryWireConnector connector)) return;
        if (!(level.getBlockEntity(connectorPos) instanceof WireConnectorBlockEntity)) return;
        if (!connector.canConnectWire(level, connectorPos, state)) return;

        // P&W's own node-data path. Do not create a second OHE wire graph.
        cir.setReturnValue(new BlockConnectorNodeData(connectorPos));
    }

    private static BlockPos resolveConnector(Level level, BlockPos hitPos) {
        BlockState state = level.getBlockState(hitPos);
        if (state.getBlock() instanceof OhePowerBridgeBlock
                || state.getBlock() instanceof OheSectionAssemblyBlock
                || state.getBlock() instanceof OheSwitchAssemblyBlock
                || state.getBlock() instanceof CatenaryHolderBlock) {
            return hitPos;
        }

        // Targeting either visible insulator on the Power Bridge must resolve to
        // the actual P&W connector block at the top of the three-block assembly.
        if (state.is(ModBlocks.OHE_SECTION_INSULATOR.get())) {
            for (int i = 1; i <= 2; i++) {
                BlockPos p = hitPos.above(i);
                if (level.getBlockState(p).getBlock() instanceof OhePowerBridgeBlock) return p;
            }
        }
        return null;
    }
}
