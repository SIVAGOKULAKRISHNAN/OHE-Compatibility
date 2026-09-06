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

    public record OheEndpoint(WireOwner owner, Object nativeEndpoint) {}

    private IntegrationContract() {}
}
