package com.gokul.ohecompat.integration;

import net.neoforged.fml.ModList;

public final class PawBridge {
    private PawBridge() {}

    public static boolean present() {
        return ModList.get().isLoaded("pantographsandwires");
    }

    public static String wireConnectorRegistry() {
        return "de.mrjulsen.wires.WiresApi.WIRE_CONNECTOR";
    }

    public static String cantileverConnectorRegistry() {
        return "de.mrjulsen.wires.WiresApi.CANTILEVER_WIRE_CONNECTOR";
    }
}
