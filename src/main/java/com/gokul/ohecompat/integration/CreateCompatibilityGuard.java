package com.gokul.ohecompat.integration;

import net.neoforged.fml.ModList;

public final class CreateCompatibilityGuard {
    public static boolean createLoaded() {
        return ModList.get().isLoaded("create");
    }

    /**
     * The dependency metadata already constrains us to Create 6.x.x.
     * This guard exists so optional compatibility code can fail closed.
     */
    public static boolean isCreate6Family() {
        if (!createLoaded()) return false;
        return true;
    }

    private CreateCompatibilityGuard() {}
}
