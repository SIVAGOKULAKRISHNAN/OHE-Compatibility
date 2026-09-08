package com.gokul.ohecompat.block;

import com.george_vi.electroenergetics.content.connector.ConnectorBlock;
import com.gokul.ohecompat.integration.PawNativeConnector;
import com.gokul.ohecompat.integration.PawOhePlacement;
import com.gokul.ohecompat.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** CEE-to-P&W feeder interface; the conductor is P&W Energy Wire. */
public final class OheFeederBridgeBlock extends ConnectorBlock implements IWireConnector, EntityBlock {
    public static final MapCodec<OheFeederBridgeBlock> CODEC = simpleCodec(OheFeederBridgeBlock::new);
    public static final BooleanProperty OPTION_TWO = BooleanProperty.create("option_two");

    @Override protected MapCodec<? extends ConnectorBlock> codec() { return CODEC; }

    public OheFeederBridgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
                .setValue(STYLE, Style.SHORT).setValue(OPTION_TWO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPTION_TWO);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        if (!PawOhePlacement.onEnergyWire(context.getLevel(), context.getClickedPos())) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    private static Vec3 ceeEndpoint(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(0.5, 0.5, 0.0);
            case SOUTH -> new Vec3(0.5, 0.5, 1.0);
            case EAST -> new Vec3(1.0, 0.5, 0.5);
            case WEST -> new Vec3(0.0, 0.5, 0.5);
            case UP -> new Vec3(0.5, 1.0, 0.5);
            case DOWN -> new Vec3(0.5, 0.0, 0.5);
        };
    }

    private static Vec3 pawEndpoint(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(0.5, 0.5, 1.0);
            case SOUTH -> new Vec3(0.5, 0.5, 0.0);
            case EAST -> new Vec3(0.0, 0.5, 0.5);
            case WEST -> new Vec3(1.0, 0.5, 0.5);
            case UP -> new Vec3(0.5, 0.0, 0.5);
            case DOWN -> new Vec3(0.5, 1.0, 0.5);
        };
    }

    public Vec3 getConnectorPoint(Level level, BlockPos pos, BlockState state) { return pawEndpoint(state.getValue(FACING)); }

    @Override public java.util.Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        return java.util.Map.of(0, ceeEndpoint(state.getValue(FACING)));
    }
    @Override public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int node) {
        return node == 0 ? ceeEndpoint(state.getValue(FACING)) : Vec3.ZERO;
    }
    @Override public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int index) {
        Vec3 p = pawEndpoint(level.getBlockState(pos).getValue(FACING));
        return index == 0 ? PawNativeConnector.point(new org.joml.Vector3d(p.x, p.y, p.z)) : new ConnectorDataProvider.Empty();
    }
    @Override public boolean canConnectWire(LevelReader level, BlockPos pos, BlockState state) {
        return com.gokul.ohecompat.integration.PawBridge.present();
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.createPawFeederBridgeBlockEntity(pos, state);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                          net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            if (!level.isClientSide) level.setBlock(pos, state.cycle(OPTION_TWO), Block.UPDATE_ALL);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public BlockState getRotatedBlockState(BlockState state, Direction direction) {
        if (direction.getAxis().isHorizontal()) return state.setValue(FACING, direction);
        return state;
    }
}
