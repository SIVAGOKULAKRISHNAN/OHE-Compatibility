package com.gokul.ohecompat.block;

import com.mojang.serialization.MapCodec;
import com.gokul.ohecompat.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import com.simibubi.create.AllItems;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Visible insulator part of the three-block OHE Power Bridge assembly. */
public class OheSectionInsulatorBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty SEPARATED = BooleanProperty.create("separated");
    public static final IntegerProperty MOUNT_SIZE = IntegerProperty.create("mount_size", 1, 3);
    public static final MapCodec<OheSectionInsulatorBlock> CODEC = simpleCodec(OheSectionInsulatorBlock::new);

    @Override
    protected MapCodec<? extends OheSectionInsulatorBlock> codec() { return CODEC; }

    public OheSectionInsulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SEPARATED, true).setValue(MOUNT_SIZE, 2));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SEPARATED, MOUNT_SIZE);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing).setValue(MOUNT_SIZE, getMountSize(context.getLevel(), context.getClickedPos().below()));
    }

    public static int getMountSize(Level level, BlockPos supportPos) {
        if (level == null || !level.isLoaded(supportPos)) return 2;
        try {
            VoxelShape shape = level.getBlockState(supportPos).getShape(level, supportPos);
            net.minecraft.world.phys.AABB box = shape.bounds();
            double width = Math.max(box.getXsize(), box.getZsize()) * 16.0D;
            return width <= 5.0D ? 1 : (width <= 10.0D ? 2 : 3);
        } catch (Exception ignored) {
            return 2;
        }
    }

    private static BlockPos findBridge(Level level, BlockPos pos) {
        for (int i = 1; i <= 2; i++) {
            BlockPos candidate = pos.above(i);
            if (level.getBlockState(candidate).is(ModBlocks.OHE_POWER_BRIDGE.get())) return candidate;
        }
        return null;
    }

    private static void breakAssembly(Level level, BlockPos pos) {
        BlockPos bridge = findBridge(level, pos);
        if (bridge == null) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        BlockPos lower = bridge.below(2);
        BlockPos upper = bridge.below();
        level.setBlock(lower, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(upper, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(bridge, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Block.popResource(level, pos, new ItemStack(ModBlocks.OHE_POWER_BRIDGE.get()));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.is(AllItems.WRENCH.get())) {
            if (!level.isClientSide) breakAssembly(level, pos);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) breakAssembly(level, pos);
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return Shapes.or(
                Block.box(6, 0, 6, 10, 16, 10),
                Block.box(4, 1, 4, 12, 3, 12),
                Block.box(4, 5, 4, 12, 7, 12),
                Block.box(4, 9, 4, 12, 11, 12),
                Block.box(4, 13, 4, 12, 15, 12)
        );
    }
}
