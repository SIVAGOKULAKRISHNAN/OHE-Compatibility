package com.gokul.ohecompat.integration;

/**
 * Compatibility boundary:
 * CEE owns electrical simulation/train behavior.
 * P&W owns physical catenary graph, geometry, tension and pantograph contact.
 */
public final class IntegrationContract {
    public enum SectionMode {
        SEPARATED, CONNECTED, LEFT_FEED, RIGHT_FEED
    }

    public enum WireOwner {
        CEE, PAW
    }

    /** Native P&W conductor classes mirrored only as invisible CEE electrical paths. */
    public enum PawCircuit {
        OHE_CATENARY,
        FEEDER_ENERGY
    }

    public record OheEndpoint(WireOwner owner, Object nativeEndpoint) {}

    private IntegrationContract() {}
}
