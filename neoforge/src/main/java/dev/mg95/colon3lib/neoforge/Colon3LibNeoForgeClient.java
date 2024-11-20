package dev.mg95.colon3lib.neoforge;

import dev.mg95.colon3lib.ui.ConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import dev.mg95.colon3lib.Colon3Lib;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Colon3Lib.MOD_ID, dist = Dist.CLIENT)
public final class Colon3LibNeoForgeClient {
    public Colon3LibNeoForgeClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parent) -> ConfigScreen.build(Colon3Lib.CONFIG, parent));
    }
}
