package dev.mg95.colon3lib.fabric;

import dev.mg95.colon3lib.Colon3Lib;
import net.fabricmc.api.ModInitializer;


public final class Colon3LibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        Colon3Lib.init();
    }
}
