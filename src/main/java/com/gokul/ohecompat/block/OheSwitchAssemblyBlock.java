package com.gokul.ohecompat.block;

import com.mojang.serialization.MapCodec;

import com.gokul.ohecompat.integration.CeeSwitchAdapter;
import com.gokul.ohecompat.registry.ModBlocks;
import com.gokul.ohecompat.integration.PawNativeConnector;
import com.gokul.ohecompat.blockentity.PawConnectorBlockEntity;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;

public final class OheSwitchAssemblyBlock extends HorizontalDirectionalBlock implements net.minecraft.world.level.block.EntityBlock, IWireConnector {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HANDLE_DOWN = BooleanProperty.create("handle_down");

    public static final MapCodec<OheSwitchAssemblyBlock> CODEC = simpleCodec(OheSwitchAssemblyBlock::new);

    @Override
    protected MapCodec<? extends OheSwitchAssemblyBlock> codec() { return CODEC; }

    public OheSwitchAssemblyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HANDLE_DOWN, true));
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANDLE_DOWN);
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        return PawNativeConnector.sectionPoint(level.getBlockState(pos).getValue(FACING), connectionPointIndex);
    }

    @Override
    public boolean canConnectWire(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
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
        if (!CeeSwitchAdapter.isWrench(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

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
