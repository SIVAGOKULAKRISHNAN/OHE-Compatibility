package com.gokul.ohecompat.mixin;

import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryHolderBlock;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes CEE's railway catenary holder a native P&W wire endpoint.
 *
 * The P&W graph node itself is placed at CEE's own catenary node position,
 * so the P&W contact endpoint and CEE electrical/visual endpoint are identical.
 * The CEE holder does not advertise a P&W cantilever tension point.
 */
@Mixin(CatenaryHolderBlock.class)
public abstract class CeeCatenaryHolderWireConnectorMixin implements IWireConnector {
    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int index) {
        // A CEE holder is a fixed OHE contact, not a P&W cantilever.
        // Do NOT expose a P&W cantilever tension attachment here: that creates
        // an extra/floating line when a P&W catenary is attached to the CEE holder.
        // The P&W graph node is already stored at CEE's exact contact position.
        return new BasicConnectorDataProvider(new Vector3f(0.0f, 0.0f, 0.0f));
    }

    @Override
    public boolean canConnectWire(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }
}
