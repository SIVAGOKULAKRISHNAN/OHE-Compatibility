package com.gokul.ohecompat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class OheConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_SHADOW_CEE_NETWORK;
    public static final ModConfigSpec.BooleanValue ENABLE_PAW_PANTOGRAPH_BRIDGE;
    public static final ModConfigSpec.BooleanValue ENABLE_COMPACT_SUBSTATION;
    public static final ModConfigSpec.BooleanValue ENABLE_OHE_BOOSTER;
    public static final ModConfigSpec.BooleanValue ALLOW_SECTION_PARALLELING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("ohe_compatibility");
        ENABLE_SHADOW_CEE_NETWORK = b.comment("Mirror P&W catenary topology into CEE's electrical simulation without changing P&W wire geometry.")
                .define("shadow_cee_network", true);
        ENABLE_PAW_PANTOGRAPH_BRIDGE = b.comment("Expose P&W pantographs to CEE's native train electrical simulation.")
                .define("paw_pantograph_bridge", true);
        ENABLE_COMPACT_SUBSTATION = b.comment("Enable the compact railway physical arrangement using the native CEE transformer behavior.")
                .define("compact_substation", true);
        ENABLE_OHE_BOOSTER = b.comment("Enable the new optional OHE transfer/booster transformer topology.")
                .define("ohe_booster", true);
        ALLOW_SECTION_PARALLELING = b.comment("Allow controlled paralleling of two electrically compatible OHE sections.")
                .define("allow_section_paralleling", false);
        b.pop();
        SPEC = b.build();
    }

    private OheConfig() {}
}
