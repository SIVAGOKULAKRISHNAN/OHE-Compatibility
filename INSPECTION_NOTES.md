# API inspection — supplied JARs

## CEE 1.21.1-1.1.1

Confirmed:
- HVSwitchBlock extends SimpleElectricalDeviceBlock<HVSwitchDevice> and IBE<HVSwitchBlockEntity>.
- HVSwitchBlock exposes FACING, POWERED, getDevice(), getStateForPlacement(), getNodePositions(), getNodePosition(), getBlockEntityClass(), getBlockEntityType().
- HVSwitchDevice contains state, target, progress, connecting/arc behavior.
- HVSwitchBlockEntity contains connected/arcing/progress state and native tick/audio behavior.
- TransformerBlock extends SimpleElectricalDeviceBlock<TransformerDevice>.
- TransformerBlock exposes getDevice(), getNodePositions(), getNodePosition(), getBlockEntityClass(), getBlockEntityType(), getDefaultDeviceData().
- TransformerDevice and TransformerBlockEntity implement the native CEE transformer simulation/controls.
- InfrastructureSavedData exposes registerOrUpdateNodes(), connect(), removeConnection(), createDetachedNode(), getNodeData(), getConnections(), isConnected().
- CatenaryConnectionData extends WireData.
- CatenaryModule consumes CEE TrainPantographEntry objects and CEE WireSimulationState.
- IPantographBlock is the bridge interface used by CEE's CarriageContraptionMixin.

## P&W beta-0.2.3-C6

Confirmed:
- WiresApi.PAW_CATENARY_WIRES.
- WireGraphManager.get(Level, GraphId).
- WireGraph.getNodes(), getEdges(), getNode(UUID).
- WireNode exposes UUID and world position.
- WireEdge exposes UUID, type, nodeA/nodeB UUIDs.
- P&W PantographBlock is a normal block with a PantographBlockEntity; it does not implement CEE IPantographBlock natively.

## Architecture conclusion

The safest compatibility strategy is:
P&W physical graph -> CEE electrical shadow graph,
while P&W pantograph is exposed to CEE's existing TrainPantographEntry capture through a mixin.

This avoids replacing either mod's native OHE implementation.
