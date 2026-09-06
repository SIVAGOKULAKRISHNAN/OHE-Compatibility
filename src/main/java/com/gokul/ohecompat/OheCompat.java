package com.gokul.ohecompat;

import com.gokul.ohecompat.config.OheConfig;
import com.gokul.ohecompat.registry.ModBlocks;
import com.gokul.ohecompat.registry.ModCreativeTabs;
import com.gokul.ohecompat.integration.PawShadowCatenaryManager;
import com.gokul.ohecompat.integration.PawShadowWireType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;

@Mod(OheCompat.MOD_ID)
public final class OheCompat {
    public static final String MOD_ID = "ohecompat";

    public OheCompat(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModCreativeTabs.register(modBus);
        ModContainer container = ModList.get().getModContainerById(MOD_ID).orElseThrow();
        container.registerConfig(ModConfig.Type.COMMON, OheConfig.SPEC);
        PawShadowWireType.register(modBus);
        PawShadowCatenaryManager.register(modBus);
    }
}
