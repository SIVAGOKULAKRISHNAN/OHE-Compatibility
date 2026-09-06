package com.gokul.ohecompat.integration;

/**
 * Runtime integration boundary. CEE owns electrical simulation/train behavior;
 * P&W owns wire geometry, tension, WireGraph and pantograph contact.
 */
public final class NativeAdapters {
    public interface CeeElectrical {
        Object connect(Object a, Object b);
        void disconnect(Object connection);
    }
    public interface PawWireGraph {
        Object connect(Object a, Object b);
        void disconnect(Object connection);
        boolean compatible(Object a, Object b);
    }
    public interface PantographBridge {
        Object electricalEndpoint(Object pantograph);
    }
    private NativeAdapters() {}
}
