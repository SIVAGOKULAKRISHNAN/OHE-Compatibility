package com.gokul.ohecompat.item;

import com.gokul.ohecompat.block.OheJunctionLineBlock;
import com.gokul.ohecompat.blockentity.OheJunctionLineBlockEntity;
import com.gokul.ohecompat.integration.PawWireSelection;
import com.gokul.ohecompat.registry.ModBlocks;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.GraphId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import de.mrjulsen.wires.item.IWireInteractableItem;

import java.util.Optional;

public final class OheJunctionLineItem extends BlockItem implements IWireInteractableItem {
    public static final String NBT_SELECTION_A = "JunctionSelectionA";

    public OheJunctionLineItem(OheJunctionLineBlock block, Item.Properties properties) {
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
            player.displayClientMessage(Component.literal("Junction Line: P&W graph is unavailable."), true);
            return InteractionResult.FAIL;
        }

        if (edge == null || !"pantographsandwires:catenary_wire".equals(edge.getType().getRegistryId().toString())) {
            player.displayClientMessage(Component.literal("Junction Line: select a P&W OHE catenary/contact wire."), true);
            return InteractionResult.FAIL;
        }

        String wireName = hit.getWireId().name();
        if (!"contact".equals(wireName)) {
            player.displayClientMessage(Component.literal("Junction Line: select the P&W contact conductor."), true);
            return InteractionResult.FAIL;
        }

        Optional<PawWireSelection> selection = PawWireSelection.fromHit(server, hit);
        if (selection.isEmpty()) {
            player.displayClientMessage(Component.literal("Junction Line: the selected P&W point could not be resolved."), true);
            return InteractionResult.FAIL;
        }

        ItemStack stack = player.getItemInHand(hand);
        CompoundTag stackTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Optional<PawWireSelection> first = stackTag.contains(NBT_SELECTION_A)
                ? PawWireSelection.fromNbt(stackTag.getCompound(NBT_SELECTION_A))
                : Optional.empty();

        if (first.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.put(NBT_SELECTION_A, selection.get().toNbt()));
            player.displayClientMessage(Component.literal(
                    "Junction A selected • click the second P&W OHE line for B"), true);
            return InteractionResult.SUCCESS;
        }

        PawWireSelection a = first.get();
        PawWireSelection b = selection.get();
        if (a.edgeId().equals(b.edgeId()) && a.graphId().equals(b.graphId())) {
            player.displayClientMessage(Component.literal("Junction Line: A and B must be different P&W OHE edges."), true);
            return InteractionResult.FAIL;
        }

        Optional<Vector3d> pa = a.exactPosition(server);
        Optional<Vector3d> pb = b.exactPosition(server);
        if (pa.isEmpty() || pb.isEmpty()) {
            player.displayClientMessage(Component.literal("Junction Line: one selected P&W point is no longer valid."), true);
            return InteractionResult.FAIL;
        }

        BlockPos host = findPlaceableHost(server, midpoint(pa.get(), pb.get()));
        if (host == null) {
            player.displayClientMessage(Component.literal("Junction Line: no replaceable block was found near the selected OHE lines."), true);
            return InteractionResult.FAIL;
        }

        Direction facing = horizontalDirection(pa.get(), pb.get());
        server.setBlock(host, ModBlocks.OHE_JUNCTION_LINE.get().defaultBlockState()
                .setValue(OheJunctionLineBlock.FACING, facing),
                net.minecraft.world.level.block.Block.UPDATE_ALL);

        if (server.getBlockEntity(host) instanceof OheJunctionLineBlockEntity be) {
            be.setSelections(a, b);
            be.setChanged();
            server.sendBlockUpdated(host, server.getBlockState(host), server.getBlockState(host),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        } else {
            server.removeBlock(host, false);
            player.displayClientMessage(Component.literal("Junction Line: block entity creation failed."), true);
            return InteractionResult.FAIL;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.remove(NBT_SELECTION_A));
        if (!player.isCreative() && !player.isSpectator()) {
            stack.shrink(1);
        }

        player.displayClientMessage(Component.literal("Junction Line created: P&W OHE A → P&W OHE B."), true);
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
