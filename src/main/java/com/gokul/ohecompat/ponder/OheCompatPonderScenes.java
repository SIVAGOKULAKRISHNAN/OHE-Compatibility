package com.gokul.ohecompat.ponder;

import com.gokul.ohecompat.registry.ModBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;

/**
 * Short Ponder guides for the explicit P&W selection workflows.
 *
 * The scenes intentionally teach the topology without spawning a fake wire:
 * P&W remains the physical wire owner and the compatibility layer records
 * exact wire selections.
 */
public final class OheCompatPonderScenes {

    public static void feederUpper(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("feeder_upper", "OHE Feeder Bridge - Upper OHE pickup");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.world().setBlock(util.grid().at(2, 1, 2), ModBlocks.OHE_FEEDER_BRIDGE.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 2, 2), ModBlocks.OHE_POWER_BRIDGE.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 2, 2), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(util.grid().at(2, 2, 2)))
                .placeNearTarget()
                .text("Select the real P&W OHE contact/tension conductor at the exact pickup point.");
        scene.idle(80);

        scene.overlay().showText(70)
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget()
                .text("The bridge stores the P&W graph, wire, percentage and channel. No nearest-node guess is used.");
        scene.idle(80);

        scene.overlay().showText(60)
                .colored(PonderPalette.GREEN)
                .text("Electrical path: CEE source -> Power Bridge -> native P&W Energy Wire -> Feeder Bridge -> P&W OHE")
                .pointAt(util.vector().centerOf(util.grid().at(2, 2, 2)));
        scene.idle(70);
    }

    public static void feederEnergy(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("feeder_energy", "OHE Feeder Bridge - P&W Energy Wire");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.world().setBlock(util.grid().at(2, 1, 2), ModBlocks.OHE_FEEDER_BRIDGE.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(1, 2, 2), ModBlocks.OHE_FEEDER_BRIDGE.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(1, 2, 2), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(util.grid().at(1, 2, 2)))
                .placeNearTarget()
                .text("First select the native P&W Energy Wire, or select OHE first and Energy Wire second.");
        scene.idle(80);

        scene.overlay().showText(70)
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget()
                .text("Second select the native P&W OHE. The exact Energy Wire and OHE points are stored in the bridge.");
        scene.idle(80);

        scene.overlay().showPalette(PonderPalette.GREEN, 60)
                .text("Only the selected P&W circuits are joined through the CEE electrical shadow node.");
        scene.idle(70);
    }

    public static void junctionLine(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("junction_line", "Junction Line - OHE A to OHE B");
        scene.configureBasePlate(0, 0, 7);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.world().setBlock(util.grid().at(2, 1, 3), ModBlocks.OHE_JUNCTION_LINE.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(3, 1, 3), ModBlocks.OHE_JUNCTION_LINE.get().defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(2, 1, 3, 3, 1, 3), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 3)))
                .placeNearTarget()
                .text("Using the Junction Line tool, click the first P&W OHE conductor: this becomes A.");
        scene.idle(80);

        scene.overlay().showText(70)
                .pointAt(util.vector().centerOf(util.grid().at(4, 1, 3)))
                .placeNearTarget()
                .text("Click the second P&W OHE conductor: this becomes B. Both selections retain exact graph, wire and percentage data.");
        scene.idle(80);

        scene.overlay().showText(70)
                .colored(PonderPalette.GREEN)
                .text("Electrical topology: A Contact -> A Catenary -> B Catenary -> B Contact. Other P&W channels stay independent.")
                .pointAt(util.vector().centerOf(util.grid().at(3, 2, 3)));
        scene.idle(80);
    }

    private OheCompatPonderScenes() {}
}
