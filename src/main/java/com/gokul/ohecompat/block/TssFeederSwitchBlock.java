package com.gokul.ohecompat.block;

import com.gokul.ohecompat.integration.PawNativeConnector;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;

/**
 * Universal P&W-side traction-substation feeder isolator.
 *
 * P&W owns the physical Energy Wire connection. The compatibility layer mirrors
 * those exact P&W endpoints into CEE's electrical simulation, so the same TSS
 * switch can sit between CEE equipment, a P&W feeder, or the C/D compatibility
 * assemblies without requiring a second CEE-only block.
 *
 * It is an isolator/disconnector, not a circuit breaker. Empty-hand interaction
 * toggles the physical open/closed state; the compatibility manager applies the
 * corresponding electrical gap/continuity to the CEE shadow network.
 */
public final class TssFeederSwitchBlock extends HorizontalDirectionalBlock
        implements EntityBlock, IWireConnector {
    public static final MapCodec<TssFeederSwitchBlock> CODEC = simpleCodec(TssFeederSwitchBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public TssFeederSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, true));
    }

    @Override
    protected MapCodec<? extends TssFeederSwitchBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(OPEN, true);
    }

    /** Two P&W Energy Wire terminals, opposite each other. */
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

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state,
                                        CustomData customData, int index) {
        return index >= 0 && index <= 1 ? endpoint(state.getValue(FACING), index) : Vec3.ZERO;
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
                level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_ALL);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static boolean is(BlockState state) {
        return state.is(ModBlocks.TSS_FEEDER_SWITCH.get());
    }
}
