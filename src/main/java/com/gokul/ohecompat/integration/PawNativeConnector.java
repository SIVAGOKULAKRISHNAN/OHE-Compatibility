package com.gokul.ohecompat.integration;

import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.Direction;
import org.joml.Vector3d;

/** Small, version-locked adapter for P&W's public connector API. */
public final class PawNativeConnector {
    private PawNativeConnector() {}

    public static ConnectorDataProvider point(Vector3d offset) {
        return new BasicConnectorDataProvider(offset);
    }

    /** External point on the opposite face from the CEE terminal. */
    public static ConnectorDataProvider bridgePoint(Direction facing, CustomData customData, int index) {
        if (index != 0) return new ConnectorDataProvider.Empty();
        return point(switch (facing) {
            case UP -> new Vector3d(0.5, 0.0, 0.5);
            case DOWN -> new Vector3d(0.5, 1.0, 0.5);
            case NORTH -> new Vector3d(0.5, 0.5, 1.0);
            case SOUTH -> new Vector3d(0.5, 0.5, 0.0);
            case EAST -> new Vector3d(0.0, 0.5, 0.5);
            case WEST -> new Vector3d(1.0, 0.5, 0.5);
        });
    }

    /** Two external endpoints for C1-C4 section/neutral assemblies and D. */
    public static ConnectorDataProvider sectionPoint(Direction facing, int index) {
        if (index != 0 && index != 1) return new ConnectorDataProvider.Empty();
        boolean first = index == 0;
        return switch (facing) {
            case NORTH -> point(new Vector3d(0.5, 0.5, first ? 0.0 : 1.0));
            case SOUTH -> point(new Vector3d(0.5, 0.5, first ? 1.0 : 0.0));
            case EAST -> point(new Vector3d(first ? 1.0 : 0.0, 0.5, 0.5));
            case WEST -> point(new Vector3d(first ? 0.0 : 1.0, 0.5, 0.5));
            case UP -> point(new Vector3d(0.5, first ? 1.0 : 0.0, 0.5));
            case DOWN -> point(new Vector3d(0.5, first ? 0.0 : 1.0, 0.5));
        };
    }
}
