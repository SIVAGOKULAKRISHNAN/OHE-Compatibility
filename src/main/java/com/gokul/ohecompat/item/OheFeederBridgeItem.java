package com.gokul.ohecompat.item;

import com.gokul.ohecompat.block.OheFeederBridgeBlock;
import com.gokul.ohecompat.blockentity.OheFeederBridgeBlockEntity;
import com.gokul.ohecompat.integration.PawWireSelection;
import com.gokul.ohecompat.registry.ModBlocks;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import de.mrjulsen.wires.item.IWireInteractableItem;

import java.util.Optional;

public final class OheFeederBridgeItem extends BlockItem implements IWireInteractableItem {
    public static final String NBT_FIRST = "FeederFirstSelection";

    public OheFeederBridgeItem(OheFeederBridgeBlock block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.FAIL;

        WireGraph graph;
        WireEdge edge;
        try {
            graph = WireGraphManager.get(server, hit.getGraphId());
            edge = graph.getEdge(hit.getWireId().id());
        } catch (Exception e) {
            player.displayClientMessage(Component.literal("Feeder Bridge: P&W graph is unavailable."), true);
            return InteractionResult.FAIL;
        }

        if (edge == null) {
            player.displayClientMessage(Component.literal("Feeder Bridge: selected P&W wire no longer exists."), true);
            return InteractionResult.FAIL;
        }

        String type = edge.getType().getRegistryId().toString();
        boolean energy = "pantographsandwires:energy_wire".equals(type);
        boolean ohe = "pantographsandwires:catenary_wire".equals(type);
        if (!energy && !ohe) {
            player.displayClientMessage(Component.literal("Feeder Bridge: select P&W Energy Wire or P&W OHE."), true);
            return InteractionResult.FAIL;
        }
        if (ohe && !"contact".equals(hit.getWireId().name())) {
            player.displayClientMessage(Component.literal("Feeder Bridge: select the P&W OHE contact conductor."), true);
            return InteractionResult.FAIL;
        }

        Optional<PawWireSelection> selection = PawWireSelection.fromHit(server, hit);
        if (selection.isEmpty()) {
            player.displayClientMessage(Component.literal("Feeder Bridge: the selected P&W point could not be resolved."), true);
            return InteractionResult.FAIL;
        }

        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();
        Optional<PawWireSelection> first = tag.contains(NBT_FIRST)
                ? PawWireSelection.fromNbt(tag.getCompound(NBT_FIRST))
                : Optional.empty();

        if (first.isEmpty()) {
            tag.put(NBT_FIRST, selection.get().toNbt());
            player.displayClientMessage(Component.literal(
                    energy ? "Energy Wire selected • click the P&W OHE for the bridge pickup."
                           : "OHE selected • click the P&W Energy Wire for the bridge pickup."), true);
            return InteractionResult.SUCCESS;
        }

        PawWireSelection s1 = first.get();
        PawWireSelection s2 = selection.get();
        Optional<WireEdge> e1 = s1.edge(server);
        Optional<WireEdge> e2 = s2.edge(server);
        if (e1.isEmpty() || e2.isEmpty()) {
            player.displayClientMessage(Component.literal("Feeder Bridge: the first P&W selection is no longer valid."), true);
            return InteractionResult.FAIL;
        }

        boolean firstEnergy = "pantographsandwires:energy_wire".equals(e1.get().getType().getRegistryId().toString());
        boolean secondEnergy = "pantographsandwires:energy_wire".equals(e2.get().getType().getRegistryId().toString());
        if (firstEnergy == secondEnergy) {
            player.displayClientMessage(Component.literal(
                    "Feeder Bridge: select one P&W Energy Wire and one P&W OHE line."), true);
            return InteractionResult.FAIL;
        }

        PawWireSelection energySel = firstEnergy ? s1 : s2;
        PawWireSelection oheSel = firstEnergy ? s2 : s1;

        Optional<Vector3d> pe = energySel.exactPosition(server);
        Optional<Vector3d> po = oheSel.exactPosition(server);
        if (pe.isEmpty() || po.isEmpty()) {
            player.displayClientMessage(Component.literal("Feeder Bridge: one P&W endpoint is no longer valid."), true);
            return InteractionResult.FAIL;
        }

        BlockPos host = findPlaceableHost(server, midpoint(pe.get(), po.get()));
        if (host == null) {
            player.displayClientMessage(Component.literal("Feeder Bridge: no replaceable block was found near the selected wires."), true);
            return InteractionResult.FAIL;
        }

        Direction facing = horizontalDirection(pe.get(), po.get());
        server.setBlock(host, ModBlocks.OHE_FEEDER_BRIDGE.get().defaultBlockState()
                .setValue(OheFeederBridgeBlock.FACING, facing),
                net.minecraft.world.level.block.Block.UPDATE_ALL);

        if (server.getBlockEntity(host) instanceof OheFeederBridgeBlockEntity be) {
            be.setSelections(energySel, oheSel);
            be.setChanged();
            server.sendBlockUpdated(host, server.getBlockState(host), server.getBlockState(host),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        } else {
            server.removeBlock(host, false);
            player.displayClientMessage(Component.literal("Feeder Bridge: block entity creation failed."), true);
            return InteractionResult.FAIL;
        }

        tag.remove(NBT_FIRST);
        stack.setTag(tag);
        if (!player.isCreative() && !player.isSpectator()) stack.shrink(1);

        player.displayClientMessage(Component.literal(
                "Feeder Bridge linked: P&W Energy Wire → Feeder Bridge → P&W OHE."), true);
        return InteractionResult.SUCCESS;
    }

    private static Vector3d midpoint(Vector3d a, Vector3d b) {
        return new Vector3d(a).add(b).mul(0.5D);
    }

    private static Direction horizontalDirection(Vector3d a, Vector3d b) {
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        if (Math.abs(dx) >= Math.abs(dz)) return dx >= 0 ? Direction.EAST : Direction.WEST;
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static BlockPos findPlaceableHost(ServerLevel level, Vector3d center) {
        BlockPos base = BlockPos.containing(center.x, center.y, center.z);
        double best = Double.MAX_VALUE;
        BlockPos bestPos = null;
        for (int r = 0; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos p = base.offset(dx, dy, dz);
                        if (!level.getBlockState(p).canBeReplaced()) continue;
                        double d = p.getCenter().distanceToSqr(center.x, center.y, center.z);
                        if (d < best) {
                            best = d;
                            bestPos = p;
                        }
                    }
                }
            }
            if (bestPos != null) return bestPos;
        }
        return null;
    }
}
