package com.gokul.ohecompat.ponder;

import com.gokul.ohecompat.OheCompat;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class OheCompatPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return OheCompat.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation feeder = ResourceLocation.fromNamespaceAndPath(OheCompat.MOD_ID, "ohe_feeder_bridge");
        ResourceLocation junction = ResourceLocation.fromNamespaceAndPath(OheCompat.MOD_ID, "ohe_junction_line");

        helper.addStoryBoard(feeder, "feeder/upper", OheCompatPonderScenes::feederUpper);
        helper.addStoryBoard(feeder, "feeder/energy", OheCompatPonderScenes::feederEnergy);
        helper.addStoryBoard(junction, "junction/a_to_b", OheCompatPonderScenes::junctionLine);
    }
}
