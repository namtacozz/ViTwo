package com.vitwo;

import net.fabricmc.api.ModInitializer;

public class ViTwoMod implements ModInitializer {
    private final com.vitwo.mod.ViTwoMod delegate = new com.vitwo.mod.ViTwoMod();

    @Override
    public void onInitialize() {
        delegate.onInitialize();
    }
}
