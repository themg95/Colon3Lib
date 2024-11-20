package dev.mg95.colon3lib.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.mg95.colon3lib.Colon3Lib;
import dev.mg95.colon3lib.ui.ConfigScreen;

import java.util.Map;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ConfigScreen.build(Colon3Lib.CONFIG, parent);
    }
}
