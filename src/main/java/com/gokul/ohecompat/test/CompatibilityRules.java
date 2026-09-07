package com.gokul.ohecompat.test;

public final class CompatibilityRules {
    public enum Owner { CEE, PAW }
    public enum SectionMode { SEPARATED, CONNECTED, LEFT_FEED, RIGHT_FEED }

    public static boolean validWirePair(Owner a, Owner b) {
        return a != null && b != null;
    }

    public static boolean allowsPowerTransfer(SectionMode mode, boolean leftPowered,
                                               boolean rightPowered, boolean parallelingEnabled) {
        return switch (mode) {
            case SEPARATED -> false;
            case LEFT_FEED -> leftPowered;
            case RIGHT_FEED -> rightPowered;
            case CONNECTED -> parallelingEnabled || (leftPowered ^ rightPowered);
        };
    }
    private CompatibilityRules() {}
}
