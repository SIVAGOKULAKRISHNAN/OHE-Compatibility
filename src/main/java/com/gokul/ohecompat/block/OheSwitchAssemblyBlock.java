package com.gokul.ohecompat.block;

import com.gokul.ohecompat.integration.CeeSwitchAdapter;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import com.simibubi.create.AllItems;

/** Railway OHE section/feeder switch mounted on a real P&W pole. */
public final class OheSwitchAssemblyBlock extends HorizontalDirectionalBlock
        implements EntityBlock, IWireConnector, ICatenaryWireConnector {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HANDLE_DOWN = BooleanProperty.create("handle_down");
    /** false = OHE section switching, true = feeder/power-line switching. */
    public static final BooleanProperty FEEDER_MODE = BooleanProperty.create("feeder_mode");
    public static final MapCodec<OheSwitchAssemblyBlock> CODEC = simpleCodec(OheSwitchAssemblyBlock::new);

    @Override
    protected MapCodec<? extends OheSwitchAssemblyBlock> codec() { return CODEC; }

    public OheSwitchAssemblyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HANDLE_DOWN, true).setValue(FEEDER_MODE, false));
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (!PawOhePlacement.onPawPole(context.getLevel(), pos.below())) return null;
        if (!PawOhePlacement.onCatenary(context.getLevel(), pos)) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANDLE_DOWN, FEEDER_MODE);
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
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        return PawNativeConnector.sectionPoint(level.getBlockState(pos).getValue(FACING), connectionPointIndex);
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state,
                                        CustomData customData, int index) {
        return index >= 0 && index <= 1 ? endpoint(state.getValue(FACING), index) : Vec3.ZERO;
    }

    @Override
    public boolean canConnectWire(LevelReader level, BlockPos pos, BlockState state) {
        return com.gokul.ohecompat.integration.PawBridge.present();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.createPawSwitchBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.isEmpty()) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.cycle(FEEDER_MODE), Block.UPDATE_ALL);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!CeeSwitchAdapter.isWrench(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (CeeSwitchAdapter.toggleExistingSwitch(level, pos)) {
            level.setBlock(pos, state.cycle(HANDLE_DOWN), Block.UPDATE_ALL);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.FAIL;
    }

    public static boolean is(BlockState state) {
        return state.is(ModBlocks.OHE_SWITCH_ASSEMBLY.get());
    }
}
