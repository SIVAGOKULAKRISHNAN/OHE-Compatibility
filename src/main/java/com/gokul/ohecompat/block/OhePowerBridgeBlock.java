package com.gokul.ohecompat.block;

import com.george_vi.electroenergetics.content.connector.ConnectorBlock;
import com.gokul.ohecompat.integration.PawNativeConnector;
import com.gokul.ohecompat.blockentity.PawConnectorBlockEntity;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.wires.item.CustomData;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import com.gokul.ohecompat.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import com.simibubi.create.AllItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A single CEE electrical node used as the visible hand-off point between
 * a native CEE feeder wire and the P&W OHE shadow network.
 *
 * The native CEE connector implementation remains responsible for the
 * electrical node/device. The compatibility layer only adds an invisible
 * electrical bridge from this node to a nearby P&W shadow catenary node.
 */
public final class OhePowerBridgeBlock extends ConnectorBlock implements IWireConnector, ICatenaryWireConnector, net.minecraft.world.level.block.EntityBlock {
    public static final MapCodec<OhePowerBridgeBlock> CODEC = simpleCodec(OhePowerBridgeBlock::new);

    @Override
    protected MapCodec<? extends ConnectorBlock> codec() {
        return CODEC;
    }

    public OhePowerBridgeBlock(Properties properties) {
        super(properties);
    }

    /**
     * The visible component is a two-level assembly: the upper bridge enclosure
     * and the ribbed insulator below it. Keep one selectable/collision shape for
     * the enclosure and a second lower shape for the insulator so the whole
     * assembly can be targeted as one component.
     *
     * The lower shape extends exactly two blocks downward; this matches the
     * requested in-game model used on top of the CEE SF6 breaker without moving the real
     * electrical node.
     */
    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        // The bridge block itself occupies one normal block. The two insulator
        // blocks are placed immediately below it, so no illegal negative model
        // coordinates are needed and the assembly renders correctly on the ground.
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) return;

        // A normal placement on top of the ground produces pos one block above
        // the supporting block. Move the bridge two more blocks upward and fill
        // the two-block insulator column below it.
        BlockPos bridgePos = pos.above(2);
        BlockPos insulatorLower = pos;
        BlockPos insulatorUpper = pos.above();

        if (level.getBlockState(bridgePos).canBeReplaced() &&
            level.getBlockState(insulatorUpper).canBeReplaced()) {

            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            Direction facing = state.getValue(FACING);
            level.setBlock(insulatorLower,
                    ModBlocks.OHE_SECTION_INSULATOR.get().defaultBlockState()
                            .setValue(OheSectionInsulatorBlock.FACING, facing)
                            .setValue(OheSectionInsulatorBlock.SEPARATED, false)
                            .setValue(OheSectionInsulatorBlock.MOUNT_SIZE, OheSectionInsulatorBlock.getMountSize(level, insulatorLower.below())), 3);
            level.setBlock(insulatorUpper,
                    ModBlocks.OHE_SECTION_INSULATOR.get().defaultBlockState()
                            .setValue(OheSectionInsulatorBlock.FACING, facing)
                            .setValue(OheSectionInsulatorBlock.SEPARATED, true)
                            .setValue(OheSectionInsulatorBlock.MOUNT_SIZE, OheSectionInsulatorBlock.getMountSize(level, insulatorLower.below())), 3);
            level.setBlock(bridgePos, state, 3);
            level.scheduleTick(bridgePos, this, 10);
        }
    }

    /**

     * CEE node #0 is deliberately placed at the external centre of the
     * front (south) connection terminal.  The previous implementation used
     * the block origin centre (0.5, 0.5, 0.5), which put the wire endpoint
     * inside the enclosure.
     *
     * FACING identifies the CEE-side terminal. The P&W terminal is always on
     * the opposite horizontal face, so rotating the state rotates both ends
     * together without changing the three-block height.
     */
    private static Vec3 externalCeeNode(BlockState state) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case UP -> new Vec3(0.5, 1.0, 0.5);
            case DOWN -> new Vec3(0.5, 0.0, 0.5);
            case NORTH -> new Vec3(0.5, 0.5, 0.0);
            case SOUTH -> new Vec3(0.5, 0.5, 1.0);
            case EAST -> new Vec3(1.0, 0.5, 0.5);
            case WEST -> new Vec3(0.0, 0.5, 0.5);
        };
    }

    @Override
    public java.util.Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        return java.util.Map.of(0, externalCeeNode(state));
    }

    @Override
    public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int node) {
        return externalCeeNode(state);
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        // Native P&W endpoint: external face opposite the existing CEE terminal.
        return PawNativeConnector.bridgePoint(level.getBlockState(pos).getValue(FACING), customData, connectionPointIndex);
    }

    /**
     * P&W requires an ICatenaryWireConnector for the catenary wire item to
     * accept this block as a native catenary endpoint.  Keep this attachment
     * point on the outside of the P&W-facing (north) face, opposite the CEE
     * terminal on the south face.
     */
    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        if (index != 0) return Vec3.ZERO;
        return switch (state.getValue(FACING)) {
            case UP -> new Vec3(0.5, 0.0, 0.5);
            case DOWN -> new Vec3(0.5, 1.0, 0.5);
            case NORTH -> new Vec3(0.5, 0.5, 1.0);
            case SOUTH -> new Vec3(0.5, 0.5, 0.0);
            case EAST -> new Vec3(0.0, 0.5, 0.5);
            case WEST -> new Vec3(1.0, 0.5, 0.5);
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The bridge is an upright railway component whose electrical and P&W
        // terminals must always be on opposite horizontal faces.  Use the
        // player's horizontal facing so the complete three-block assembly
        // rotates as one unit at every placement angle.
        Direction facing = context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public boolean canConnectWire(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return com.gokul.ohecompat.integration.PawBridge.present();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.createPawPowerBridgeBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        // The Create wrench is the intentional pickup/removal tool for the OHE Power Bridge.
        // Use normal block destruction so the normal OHE Power Bridge BlockItem drops.
        if (stack.is(AllItems.WRENCH.get())) {
            if (!level.isClientSide) {
                level.destroyBlock(pos, true, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // The two visible insulator blocks are part of this SH Breaker assembly.
        // Remove them without dropping separate insulator items.
        if (!level.isClientSide) {
            BlockPos lower = pos.below();
            BlockPos lower2 = pos.below(2);
            if (level.getBlockState(lower).is(ModBlocks.OHE_SECTION_INSULATOR.get())) {
                level.setBlock(lower, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (level.getBlockState(lower2).is(ModBlocks.OHE_SECTION_INSULATOR.get())) {
                level.setBlock(lower2, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level,
                     BlockPos pos, net.minecraft.util.RandomSource random) {
        super.tick(state, level, pos, random);
        // P&W-to-CEE bridging is synchronized from the native P&W graph manager.
        // Do not search for a nearby P&W node here: proximity-based linking can
        // create an unwanted/invisible CEE connection across an unrelated P&W line.
        level.scheduleTick(pos, this, 10);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction direction) {
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(FACING, direction);
        }
        return state;
    }
}
