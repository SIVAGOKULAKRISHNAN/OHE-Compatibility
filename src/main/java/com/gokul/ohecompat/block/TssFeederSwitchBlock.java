package com.gokul.ohecompat.block;

import com.george_vi.electroenergetics.CEEBlockEntityTypes;
import com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchBlock;
import com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchBlockEntity;
import com.gokul.ohecompat.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;

/**
 * CEE-side traction-substation feeder isolator.
 *
 * It intentionally extends CEE's native HV switch so the electrical device,
 * arc/motion simulation and two-terminal CEE node topology remain owned by CEE.
 * The compatibility mod only supplies the railway/TSS physical presentation.
 * It is an isolator/disconnector, not a circuit breaker.
 */
public final class TssFeederSwitchBlock extends HVSwitchBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public TssFeederSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(OPEN, true);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            ItemInteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult);
            if (!level.isClientSide && result.consumesAction()) {
                level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_ALL);
            }
            return result;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public Class<HVSwitchBlockEntity> getBlockEntityClass() {
        return HVSwitchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HVSwitchBlockEntity> getBlockEntityType() {
        return ModBlocks.TSS_FEEDER_SWITCH_BE.get();
    }
}
