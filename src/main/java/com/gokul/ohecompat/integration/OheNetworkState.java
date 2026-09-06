package com.gokul.ohecompat.integration;

public final class OheNetworkState {
    public enum SectionMode {
        SEPARATED,
        CONNECTED,
        LEFT_FEED,
        RIGHT_FEED
    }

    public enum SourceMode {
        EXISTING_CEE,
        COMPACT_CEE_SUBSTATION,
        OHE_BOOSTER
    }

    private OheNetworkState() {}

    public static boolean canBridge(SectionMode mode, boolean leftEnergized, boolean rightEnergized,
                                    boolean configuredToParallel) {
        if (mode == SectionMode.SEPARATED) return false;
        if (mode == SectionMode.CONNECTED) return configuredToParallel || (leftEnergized ^ rightEnergized);
        if (mode == SectionMode.LEFT_FEED) return leftEnergized;
        if (mode == SectionMode.RIGHT_FEED) return rightEnergized;
        return false;
    }
}
