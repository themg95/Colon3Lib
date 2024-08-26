package dev.mg95.colon3lib.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;

import java.io.File;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    public void init(Object config, String id) {
        try {
            _init(config, id);
        } catch (Exception e) {
            throw new RuntimeException("There was an error loading/initializing the " + id + " config. Please fix or remove the config file.\nStacktrace:", e);
        }
    }

    private void _init(Object config, String id) throws Exception {
        LinkedHashMap<String, Object> configValues = toMap(config);


        var file = new File("config/" + id + ".toml");


        if (file.exists()) {
            var loadedConfigValues = toMap(loadConfig(file));
            overrideConfig(configValues, loadedConfigValues);
        }

        saveConfig(file, configValues);
    }

    private LinkedHashMap<String, Object> toMap(Object obj) throws IllegalAccessException {
        var map = new LinkedHashMap<String, Object>();
        for (var field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Option.class)) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(obj));
            } else if (field.isAnnotationPresent(Category.class)) {
                map.put(field.getName(), toMap(field.get(obj)));
            }

        }
        return map;
    }

    private com.electronwill.nightconfig.core.Config toConfig(LinkedHashMap<String, Object> map) {
        var config = com.electronwill.nightconfig.core.Config.inMemory();
        for (var entry : map.entrySet()) {
            if (!(entry.getValue() instanceof LinkedHashMap<?, ?>)) {
                config.set(entry.getKey(), entry.getValue());
            } else {
                config.set(entry.getKey(), toConfig((LinkedHashMap<String, Object>) entry.getValue()));
            }

        }
        return config;
    }

    private LinkedHashMap<String, Object> overrideConfig(LinkedHashMap<String, Object> configValues, LinkedHashMap<String, Object> loadedConfigValues) {
        for (var entry : loadedConfigValues.entrySet()) {
            if (entry.getValue() instanceof LinkedHashMap) {
                var map = (LinkedHashMap<String, Object>) entry.getValue();
                map.putAll(overrideConfig(new LinkedHashMap<>(), (LinkedHashMap<String, Object>) entry.getValue()));
                configValues.put(entry.getKey(), map);
                continue;
            }
            configValues.put(entry.getKey(), entry.getValue());
        }

        return configValues;
    }

    private LinkedHashMap<String, Object> loadConfig(File configFile) {
        var config = FileConfig.of(configFile);
        config.load();
        var map = new LinkedHashMap<String, Object>(config.valueMap());
        config.close();
        return map;
    }

    private void saveConfig(File configFile, LinkedHashMap<String, Object> configValues) {
        var config = FileConfig.of(configFile);
        config.putAll(toConfig(configValues));
        config.save();
        config.close();
    }

}
