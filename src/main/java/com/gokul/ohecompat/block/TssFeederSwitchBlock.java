package com.gokul.ohecompat.block;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;

/**
 * Universal P&W-side traction-substation feeder isolator.
 *
 * The physical conductor is always P&W Energy Wire. The switch is an OHE-line
 * component, never a free-standing ground device. HEIGHT is a visual mast option
 * from 3 through 7 blocks; the top switch remains at the placed block while the
 * support mast extends downward into the selected P&W pole.
 */
public final class TssFeederSwitchBlock extends HorizontalDirectionalBlock
        implements EntityBlock, IWireConnector {
    public static final MapCodec<TssFeederSwitchBlock> CODEC = simpleCodec(TssFeederSwitchBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 3, 7);

    public TssFeederSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, true)
                .setValue(HEIGHT, 3));
    }

    @Override
    protected MapCodec<? extends TssFeederSwitchBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HEIGHT);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (!PawOhePlacement.onPawPole(context.getLevel(), pos.below())) return null;
        if (!PawOhePlacement.onEnergyWire(context.getLevel(), pos)
                && !PawOhePlacement.onCatenary(context.getLevel(), pos)) return null;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(OPEN, true)
                .setValue(HEIGHT, 3);
    }

    public static Vec3 endpoint(Direction facing, int index) {
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
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        return PawNativeConnector.sectionPoint(level.getBlockState(pos).getValue(FACING), connectionPointIndex);
    }

    public Vec3 getConnectorPoint(Level level, BlockPos pos, BlockState state, int index) {
        return endpoint(state.getValue(FACING), index);
    }

    @Override
    public boolean canConnectWire(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return com.gokul.ohecompat.integration.PawBridge.present();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.createPawTssFeederSwitchBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.isEmpty()) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    int next = state.getValue(HEIGHT) >= 7 ? 3 : state.getValue(HEIGHT) + 1;
                    level.setBlock(pos, state.setValue(HEIGHT, next), Block.UPDATE_ALL);
                } else {
                    level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_ALL);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static boolean is(BlockState state) {
        return state.is(ModBlocks.TSS_FEEDER_SWITCH.get());
    }
}
