package com.gokul.ohecompat.block;

import com.george_vi.electroenergetics.content.connector.ConnectorBlock;
import com.gokul.ohecompat.integration.PawNativeConnector;
import com.gokul.ohecompat.integration.PawOhePlacement;
import com.gokul.ohecompat.registry.ModBlocks;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.wires.item.CustomData;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/** CEE electrical hand-off to a native P&W catenary endpoint. */
public final class OhePowerBridgeBlock extends ConnectorBlock implements IWireConnector, ICatenaryWireConnector, net.minecraft.world.level.block.EntityBlock {
    public static final MapCodec<OhePowerBridgeBlock> CODEC = simpleCodec(OhePowerBridgeBlock::new);
    @Override protected MapCodec<? extends ConnectorBlock> codec() { return CODEC; }
    public OhePowerBridgeBlock(Properties properties) { super(properties); }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                               net.minecraft.world.phys.shapes.CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        BlockPos bridgePos = pos.above(2);
        BlockPos insulatorLower = pos;
        BlockPos insulatorUpper = pos.above();
        if (level.getBlockState(bridgePos).canBeReplaced() && level.getBlockState(insulatorUpper).canBeReplaced()) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            Direction facing = state.getValue(FACING);
            int mount = OheSectionInsulatorBlock.getMountSize(level, insulatorLower.below());
            level.setBlock(insulatorLower, ModBlocks.OHE_SECTION_INSULATOR.get().defaultBlockState()
                    .setValue(OheSectionInsulatorBlock.FACING, facing)
                    .setValue(OheSectionInsulatorBlock.SEPARATED, false)
                    .setValue(OheSectionInsulatorBlock.MOUNT_SIZE, mount), 3);
            level.setBlock(insulatorUpper, ModBlocks.OHE_SECTION_INSULATOR.get().defaultBlockState()
                    .setValue(OheSectionInsulatorBlock.FACING, facing)
                    .setValue(OheSectionInsulatorBlock.SEPARATED, true)
                    .setValue(OheSectionInsulatorBlock.MOUNT_SIZE, mount), 3);
            level.setBlock(bridgePos, state, 3);
            level.scheduleTick(bridgePos, this, 10);
        }
    }

    private static Vec3 externalCeeNode(BlockState state) {
        return switch (state.getValue(FACING)) {
            case UP -> new Vec3(0.5, 1.0, 0.5);
            case DOWN -> new Vec3(0.5, 0.0, 0.5);
            case NORTH -> new Vec3(0.5, 0.5, 0.0);
            case SOUTH -> new Vec3(0.5, 0.5, 1.0);
            case EAST -> new Vec3(1.0, 0.5, 0.5);
            case WEST -> new Vec3(0.0, 0.5, 0.5);
        };
    }

    @Override public java.util.Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        return java.util.Map.of(0, externalCeeNode(state));
    }
    @Override public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int node) { return externalCeeNode(state); }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        return PawNativeConnector.bridgePoint(level.getBlockState(pos).getValue(FACING), customData, connectionPointIndex);
    }

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

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!PawOhePlacement.onCatenary(context.getLevel(), context.getClickedPos())) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
    @Override public boolean canConnectWire(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return com.gokul.ohecompat.integration.PawBridge.present();
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.createPawPowerBridgeBlockEntity(pos, state);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.is(AllItems.WRENCH.get())) {
            if (!level.isClientSide) level.destroyBlock(pos, true, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (level.getBlockState(pos.below()).is(ModBlocks.OHE_SECTION_INSULATOR.get()))
                level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockState(pos.below(2)).is(ModBlocks.OHE_SECTION_INSULATOR.get()))
                level.setBlock(pos.below(2), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        super.tick(state, level, pos, random);
        level.scheduleTick(pos, this, 10);
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction direction) {
        if (direction.getAxis().isHorizontal()) return state.setValue(FACING, direction);
        return state;
    }
}
