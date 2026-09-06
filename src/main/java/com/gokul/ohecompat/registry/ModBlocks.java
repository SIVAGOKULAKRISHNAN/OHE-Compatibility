package com.gokul.ohecompat.registry;

import com.gokul.ohecompat.OheCompat;
import com.gokul.ohecompat.block.*;
import com.gokul.ohecompat.blockentity.OheSectionBlockEntity;
import com.gokul.ohecompat.blockentity.PawConnectorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(OheCompat.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(OheCompat.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OheCompat.MOD_ID);

    public static final DeferredBlock<Block> OHE_POWER_BRIDGE = BLOCKS.register("ohe_power_bridge",
            () -> new OhePowerBridgeBlock(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()));

    public static final DeferredBlock<Block> OHE_SECTION_INSULATOR = BLOCKS.register("ohe_section_insulator",
            () -> new OheSectionInsulatorBlock(BlockBehaviour.Properties.of().strength(1.5f).noOcclusion()));

    public static final DeferredBlock<Block> OHE_SWITCH_ASSEMBLY = BLOCKS.register("ohe_switch_assembly",
            () -> new OheSwitchAssemblyBlock(BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()));

    public static final DeferredBlock<Block> C1_CANTILEVER_NEUTRAL = BLOCKS.register("c1_cantilever_neutral",
            () -> new OheSectionAssemblyBlock(BlockBehaviour.Properties.of().strength(1.5f).noOcclusion(),
                    OheSectionAssemblyBlock.Style.C1_CANTILEVER));

    public static final DeferredBlock<Block> C2_DIRECT_SECTION = BLOCKS.register("c2_direct_section",
            () -> new OheSectionAssemblyBlock(BlockBehaviour.Properties.of().strength(1.5f).noOcclusion(),
                    OheSectionAssemblyBlock.Style.C2_DIRECT));

    public static final DeferredBlock<Block> C3_SHORT_NEUTRAL = BLOCKS.register("c3_short_neutral",
            () -> new OheSectionAssemblyBlock(BlockBehaviour.Properties.of().strength(1.5f).noOcclusion(),
                    OheSectionAssemblyBlock.Style.C3_SHORT));

    public static final DeferredBlock<Block> C4_CANTILEVER_SHORT_NEUTRAL = BLOCKS.register("c4_cantilever_short_neutral",
            () -> new OheSectionAssemblyBlock(BlockBehaviour.Properties.of().strength(1.5f).noOcclusion(),
                    OheSectionAssemblyBlock.Style.C4_CANTILEVER_SHORT));

    public static final DeferredBlock<Block> COMPACT_OHE_SUBSTATION = BLOCKS.register("compact_ohe_substation",
            () -> new CompactOheSubstationBlock(BlockBehaviour.Properties.of().strength(3.0f).noOcclusion()));

    public static final DeferredBlock<Block> OHE_BOOSTER = BLOCKS.register("ohe_booster",
            () -> new OheBoosterBlock(BlockBehaviour.Properties.of().strength(3.0f).noOcclusion()));


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PawConnectorBlockEntity>> OHE_POWER_BRIDGE_BE =
            BLOCK_ENTITIES.register("ohe_power_bridge",
                    () -> BlockEntityType.Builder.of((pos, state) -> createPawPowerBridgeBlockEntity(pos, state),
                            OHE_POWER_BRIDGE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PawConnectorBlockEntity>> OHE_SWITCH_BE =
            BLOCK_ENTITIES.register("ohe_switch",
                    () -> BlockEntityType.Builder.of((pos, state) -> createPawSwitchBlockEntity(pos, state),
                            OHE_SWITCH_ASSEMBLY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OheSectionBlockEntity>> OHE_SECTION_BE =
            BLOCK_ENTITIES.register("ohe_section",
                    () -> BlockEntityType.Builder.of((pos, state) -> createOheSectionBlockEntity(pos, state),
                            C1_CANTILEVER_NEUTRAL.get(),
                            C2_DIRECT_SECTION.get(),
                            C3_SHORT_NEUTRAL.get(),
                            C4_CANTILEVER_SHORT_NEUTRAL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlockEntity>> COMPACT_SUBSTATION_BE =
            BLOCK_ENTITIES.register("compact_ohe_substation",
                    () -> BlockEntityType.Builder.of((pos, state) -> createCompactSubstationBlockEntity(pos, state), COMPACT_OHE_SUBSTATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlockEntity>> OHE_BOOSTER_BE =
            BLOCK_ENTITIES.register("ohe_booster",
                    () -> BlockEntityType.Builder.of((pos, state) -> createOheBoosterBlockEntity(pos, state), OHE_BOOSTER.get()).build(null));

    public static PawConnectorBlockEntity createPawPowerBridgeBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new PawConnectorBlockEntity(OHE_POWER_BRIDGE_BE.get(), pos, state);
    }

    public static PawConnectorBlockEntity createPawSwitchBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new PawConnectorBlockEntity(OHE_SWITCH_BE.get(), pos, state);
    }

    // Factory methods keep the DeferredHolder self-reference out of the field initializer.
    // The factory is invoked only after the registry has been constructed.
    public static OheSectionBlockEntity createOheSectionBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new OheSectionBlockEntity(OHE_SECTION_BE.get(), pos, state);
    }

    public static com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlockEntity createCompactSubstationBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlockEntity(COMPACT_SUBSTATION_BE.get(), pos, state);
    }

    public static com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlockEntity createOheBoosterBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerBlockEntity(OHE_BOOSTER_BE.get(), pos, state);
    }

    private static void item(String id, DeferredBlock<Block> block) {
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        // Register every DeferredItem BEFORE attaching the DeferredRegister to the event bus.
        // Registering the bus first caused the compatibility blocks to exist as blocks but
        // not receive BlockItem entries, so they were invisible in the creative inventory/search.
        item("ohe_power_bridge", OHE_POWER_BRIDGE);
        item("ohe_section_insulator", OHE_SECTION_INSULATOR);
        item("ohe_switch_assembly", OHE_SWITCH_ASSEMBLY);
        item("c1_cantilever_neutral", C1_CANTILEVER_NEUTRAL);
        item("c2_direct_section", C2_DIRECT_SECTION);
        item("c3_short_neutral", C3_SHORT_NEUTRAL);
        item("c4_cantilever_short_neutral", C4_CANTILEVER_SHORT_NEUTRAL);
        item("compact_ohe_substation", COMPACT_OHE_SUBSTATION);
        item("ohe_booster", OHE_BOOSTER);

        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    private ModBlocks() {}
}
