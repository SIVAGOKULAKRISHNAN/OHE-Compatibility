package com.gokul.ohecompat.block;

import com.gokul.ohecompat.integration.PawNativeConnector;
import com.gokul.ohecompat.registry.ModBlocks;
import com.george_vi.electroenergetics.content.connector.ConnectorBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Player-facing OHE Power Bridge.
 *
 * The block is the native CEE connector/electrical node. Its P&W connector is
 * a separate native P&W endpoint supplied by the compatibility layer. The two
 * endpoints are always on opposite faces of the bridge.
 *
 * The visible component is a three-block vertical assembly:
 * bottom insulator -> bridge -> top insulator.
 * Only the bridge is an inventory item. Breaking any part removes the whole
 * assembly and produces the single bridge item.
 */
public final class OhePowerBridgeBlock extends ConnectorBlock
        implements IWireConnector, ICatenaryWireConnector, net.minecraft.world.level.block.EntityBlock {

    public static final net.minecraft.core.DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public OhePowerBridgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STYLE, Style.SHORT));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends OhePowerBridgeBlock> codec() {
        return simpleCodec(OhePowerBridgeBlock::new);
    }

    /** Player-facing horizontal orientation. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(STYLE, Style.SHORT);
    }

    /** CEE electrical terminal: the external face in FACING direction. */
    private static Vec3 externalCeeNode(BlockState state) {
        return switch (state.getValue(FACING)) {
            case NORTH -> new Vec3(0.5, 0.5, 0.0);
            case SOUTH -> new Vec3(0.5, 0.5, 1.0);
            case EAST -> new Vec3(1.0, 0.5, 0.5);
            case WEST -> new Vec3(0.0, 0.5, 0.5);
            case UP -> new Vec3(0.5, 1.0, 0.5);
            case DOWN -> new Vec3(0.5, 0.0, 0.5);
        };
    }

    @Override
    public Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        return Map.of(0, externalCeeNode(state));
    }

    @Override
    public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int node) {
        return externalCeeNode(state);
    }

    /** Native P&W connector endpoint: exactly opposite the CEE endpoint. */
    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData,
                                                   int connectionPointIndex) {
        return PawNativeConnector.bridgePoint(level.getBlockState(pos).getValue(FACING),
                customData, connectionPointIndex);
    }

    /** Native P&W catenary endpoint; no second offset is applied. */
    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state,
                                       CustomData customData, int index) {
        if (index != 0) return Vec3.ZERO;
        return switch (state.getValue(FACING)) {
            case NORTH -> new Vec3(0.5, 0.5, 1.0);
            case SOUTH -> new Vec3(0.5, 0.5, 0.0);
            case EAST -> new Vec3(0.0, 0.5, 0.5);
            case WEST -> new Vec3(1.0, 0.5, 0.5);
            case UP -> new Vec3(0.5, 0.0, 0.5);
            case DOWN -> new Vec3(0.5, 1.0, 0.5);
        };
    }

    @Override
    public boolean canConnectWire(LevelReader level, BlockPos pos, BlockState state) {
        return com.gokul.ohecompat.integration.PawBridge.present();
    }

    /** Create Wrench rotation by changing only the horizontal bridge facing. */
    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return originalState.setValue(FACING, targetedFace.getAxis().isHorizontal()
                ? targetedFace : originalState.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlocks.createPawPowerBridgeBlockEntity(pos, state);
    }

    /**
     * The normal block placement happens at the clicked support position.
     * Convert that temporary placement into the permanent three-block assembly:
     * bottom insulator at the support, bridge two blocks above it, and the
     * upper insulator between them. The bridge orientation is preserved.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        BlockPos bridgePos = pos.above(2);
        BlockPos topInsulatorPos = pos.above();
        int mountSize = OheSectionInsulatorBlock.getMountSize(level, pos.below());

        BlockState bridgeState = state.setValue(FACING, facing).setValue(STYLE, Style.SHORT);
        BlockState bottomInsulator = ModBlocks.OHE_SECTION_INSULATOR.get().defaultBlockState()
                .setValue(OheSectionInsulatorBlock.FACING, facing)
                .setValue(OheSectionInsulatorBlock.SEPARATED, false)
                .setValue(OheSectionInsulatorBlock.MOUNT_SIZE, mountSize);
        BlockState topInsulator = ModBlocks.OHE_SECTION_INSULATOR.get().defaultBlockState()
                .setValue(OheSectionInsulatorBlock.FACING, facing)
                .setValue(OheSectionInsulatorBlock.SEPARATED, true)
                .setValue(OheSectionInsulatorBlock.MOUNT_SIZE, mountSize);

        // The temporary CEE node at pos is replaced by the bottom insulator.
        // The permanent bridge gets a fresh native P&W/CEE block entity at its
        // actual three-block assembly position.
        level.setBlock(bridgePos, bridgeState, Block.UPDATE_ALL);
        level.setBlock(topInsulatorPos, topInsulator, Block.UPDATE_ALL);
        level.setBlock(pos, bottomInsulator, Block.UPDATE_ALL);
    }

    /** Remove both internal insulators before the normal single bridge drop. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos bottom = pos.below(2);
            BlockPos top = pos.below();
            if (level.getBlockState(bottom).is(ModBlocks.OHE_SECTION_INSULATOR.get())) {
                level.setBlock(bottom, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (level.getBlockState(top).is(ModBlocks.OHE_SECTION_INSULATOR.get())) {
                level.setBlock(top, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
