package com.gokul.ohecompat.integration;

import com.george_vi.electroenergetics.CEERegistries;
import com.george_vi.electroenergetics.CEEWireTypes;
import com.george_vi.electroenergetics.simulation.WireType;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Invisible CEE wire type used only for the P&W compatibility shadow network.
 *
 * The connection remains a real CEE electrical connection, but its renderer
 * receives an intentionally empty model and no endpoint model. This prevents
 * the compatibility connection from appearing as an additional normal CEE
 * wire on top of P&W's native OHE wire.
 */
public final class PawShadowWireType {
    private static final PartialModel EMPTY_MODEL = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath("ohecompat", "empty_shadow_wire")
    );

    public static final DeferredRegister<WireType> WIRE_TYPES =
            DeferredRegister.create(CEERegistries.WIRE_TYPE, "ohecompat");

    public static final DeferredHolder<WireType, WireType> SHADOW =
            WIRE_TYPES.register("paw_shadow", () -> {
                WireType standard = CEEWireTypes.STANDARD.get();
                return new WireType.Builder(EMPTY_MODEL)
                        .resistance(standard::getResistance)
                        .insulationResistance(standard.insulationResistance())
                        .maxInsulationVoltage(standard::maxInsulationVoltage)
                        .sag(0.0f)
                        .maxLength(() -> (int) Math.max(standard.getMaxLength(), 256))
                        .thickness(standard.getThickness())
                        .invulnerable()
                        .renderType(WireType.WireRenderType.SOLID)
                        .build();
            });

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        WIRE_TYPES.register(modBus);
    }

    private PawShadowWireType() {}
}
