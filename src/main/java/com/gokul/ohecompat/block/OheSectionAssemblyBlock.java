package com.gokul.ohecompat.block;

import com.mojang.serialization.MapCodec;

import com.gokul.ohecompat.blockentity.OheSectionBlockEntity;
import com.gokul.ohecompat.registry.ModBlocks;
import com.gokul.ohecompat.integration.PawNativeConnector;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import com.simibubi.create.AllItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class OheSectionAssemblyBlock extends HorizontalDirectionalBlock implements EntityBlock, IWireConnector {
    public enum Style { C1_CANTILEVER, C2_DIRECT, C3_SHORT, C4_CANTILEVER_SHORT }
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ISOLATED = BooleanProperty.create("isolated");
    private final Style style;
    public static final MapCodec<OheSectionAssemblyBlock> CODEC = simpleCodec(p -> new OheSectionAssemblyBlock(p, Style.C1_CANTILEVER));

    @Override
    protected MapCodec<? extends OheSectionAssemblyBlock> codec() { return CODEC; }


    public OheSectionAssemblyBlock(Properties properties, Style style) {
        super(properties);
        this.style = style;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ISOLATED, true));
    }

    public Style style() { return style; }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ISOLATED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.is(AllItems.WRENCH.get())) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.cycle(ISOLATED), Block.UPDATE_ALL);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.createOheSectionBlockEntity(pos, state);
    }

}
