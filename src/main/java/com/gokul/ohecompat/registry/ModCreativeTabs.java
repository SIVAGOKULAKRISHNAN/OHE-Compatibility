package com.gokul.ohecompat.registry;

import com.gokul.ohecompat.OheCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OheCompat.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OHE_TAB =
            TABS.register("ohe_compatibility", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ohecompat.ohe_compatibility"))
                    .icon(() -> new ItemStack(ModBlocks.OHE_SWITCH_ASSEMBLY.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.OHE_POWER_BRIDGE.get());
                        output.accept(ModBlocks.OHE_FEEDER_BRIDGE.get());
                        output.accept(ModBlocks.OHE_SWITCH_ASSEMBLY.get());
                        output.accept(ModBlocks.TSS_FEEDER_SWITCH.get());
                        output.accept(ModBlocks.C1_CANTILEVER_NEUTRAL.get());
                        output.accept(ModBlocks.C2_DIRECT_SECTION.get());
                        output.accept(ModBlocks.C3_SHORT_NEUTRAL.get());
                        output.accept(ModBlocks.C4_CANTILEVER_SHORT_NEUTRAL.get());
                        output.accept(ModBlocks.COMPACT_OHE_SUBSTATION.get());
                        output.accept(ModBlocks.OHE_BOOSTER.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
