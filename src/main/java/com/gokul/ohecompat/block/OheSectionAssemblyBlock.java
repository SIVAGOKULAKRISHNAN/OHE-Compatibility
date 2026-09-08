package com.gokul.ohecompat.block;

import com.george_vi.electroenergetics.content.connector.ConnectorBlock;
import com.gokul.ohecompat.blockentity.OheSectionBlockEntity;
import com.gokul.ohecompat.integration.PawNativeConnector;
import com.gokul.ohecompat.integration.PawOhePlacement;
import com.gokul.ohecompat.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.simibubi.create.AllItems;

/**
 * Common electrical/physical shell for C1-C4.
 * P&W owns the physical catenary wire. C1-C4 are only valid when placed directly
 * on an existing P&W catenary segment; they are never free-standing ground devices.
 */
public class OheSectionAssemblyBlock extends ConnectorBlock
        implements EntityBlock, IWireConnector, ICatenaryWireConnector {

    public enum Style { C1_CANTILEVER, C2_DIRECT, C3_SHORT, C4_CANTILEVER_SHORT }

    public static final BooleanProperty ISOLATED = BooleanProperty.create("isolated");
    private final Style style;
    public static final MapCodec<OheSectionAssemblyBlock> CODEC =
            simpleCodec(p -> new OheSectionAssemblyBlock(p, Style.C1_CANTILEVER));

    @Override
    protected MapCodec<? extends ConnectorBlock> codec() {
        return CODEC;
    }

    public OheSectionAssemblyBlock(Properties properties, Style style) {
        super(properties);
        this.style = style;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STYLE, ConnectorBlock.Style.SHORT)
                .setValue(ISOLATED, true));
    }

    public Style style() {
        return style;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ISOLATED);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        if (!PawOhePlacement.onCatenary(context.getLevel(), context.getClickedPos())) return null;
        Direction facing = context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public java.util.Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        return java.util.Map.of(
                0, endpoint(state.getValue(FACING), 0),
                1, endpoint(state.getValue(FACING), 1));
    }

    @Override
    public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int node) {
        if (node < 0 || node > 1) return Vec3.ZERO;
        return endpoint(state.getValue(FACING), node);
    }

    private static Vec3 endpoint(Direction facing, int index) {
        boolean first = index == 0;
        return switch (facing) {
            case NORTH -> new Vec3(0.5, 0.5, first ? 0.0 : 1.0);
            case SOUTH -> new Vec3(0.5, 0.5, first ? 1.0 : 0.0);
            case EAST -> new Vec3(first ? 1.0 : 0.0, 0.5, 0.5);
            case WEST -> new Vec3(first ? 0.0 : 1.0, 0.5, 0.5);
            case UP -> new Vec3(0.5, first ? 1.0 : 0.0, 0.5);
            case DOWN -> new Vec3(0.5, first ? 0.0 : 1.0, 0.5);
        };
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction direction) {
        if (direction.getAxis().isHorizontal()) return state.setValue(FACING, direction);
        return state;
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        return PawNativeConnector.sectionPoint(level.getBlockState(pos).getValue(FACING), connectionPointIndex);
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state,
                                        CustomData customData, int index) {
        if (index < 0 || index > 1) return Vec3.ZERO;
        return endpoint(state.getValue(FACING), index);
    }

    @Override
    public boolean canConnectWire(LevelReader level, BlockPos pos, BlockState state) {
        return com.gokul.ohecompat.integration.PawBridge.present();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.createOheSectionBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player,
                                              net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.is(AllItems.WRENCH.get())) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.cycle(ISOLATED), Block.UPDATE_ALL);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
