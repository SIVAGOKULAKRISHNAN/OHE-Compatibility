package com.gokul.ohecompat.integration;

import net.neoforged.fml.ModList;

public final class CeeBridge {
    private CeeBridge() {}

    public static boolean present() {
        return ModList.get().isLoaded("electroenergetics");
    }

    public static String trainPowerAuthority() {
        return "Create: Electro Energetics";
    }

    public static String switchFoundation() {
        return "com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchDevice";
    }

    public static String transformerFoundation() {
        return "com.george_vi.electroenergetics.content.transmission_distribution.transformer.TransformerDevice";
    }
}
