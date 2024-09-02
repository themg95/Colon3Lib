package dev.mg95.colon3lib.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;

import java.io.File;

import java.lang.reflect.InaccessibleObjectException;
import java.util.LinkedHashMap;
import java.util.Objects;

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
            var loadedConfigValues = loadConfig(file);
            configValues = overrideConfig(configValues, loadedConfigValues);
        }

        saveConfig(file, configValues);

        config = applyValues(configValues, config);
    }

    private LinkedHashMap<String, Object> toMap(Object obj) throws IllegalAccessException {
        var map = new LinkedHashMap<String, Object>();
        for (var field : obj.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
            } catch (InaccessibleObjectException e) {
                continue;
            }
            if (field.isAnnotationPresent(Option.class)) {
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

    private LinkedHashMap<String, Object> overrideConfig
            (LinkedHashMap<String, Object> configValues, LinkedHashMap<String, Object> loadedConfigValues) {
        for (var entry : configValues.entrySet()) {
            if (entry.getValue() instanceof LinkedHashMap) {
                var map = overrideConfig(
                        (LinkedHashMap<String, Object>) entry.getValue(),
                        (LinkedHashMap<String, Object>) Objects.requireNonNullElse(loadedConfigValues.get(entry.getKey()), new LinkedHashMap<String, Object>()));
                configValues.put(entry.getKey(), map);
                continue;
            }

            configValues.put(entry.getKey(), Objects.requireNonNullElse(loadedConfigValues.get(entry.getKey()), entry.getValue()));
        }

        return configValues;
    }

    private LinkedHashMap<String, Object> cleanMap(LinkedHashMap<String, Object> map) {
        for (var entry : map.entrySet()) {
            if (entry.getValue() instanceof com.electronwill.nightconfig.core.Config) {
                map.put(entry.getKey(), cleanMap(ObjectDeserializer.standard().deserializeToMap(entry.getValue(), LinkedHashMap.class, Object.class)));
            }
        }

        return map;
    }

    private LinkedHashMap<String, Object> loadConfig(File configFile) {
        var config = FileConfig.of(configFile);
        config.load();
        LinkedHashMap<String, Object> map = cleanMap(ObjectDeserializer.standard().deserializeToMap(config, LinkedHashMap.class, Object.class));
        config.close();
        return map;
    }

    private void saveConfig(File configFile, LinkedHashMap<String, Object> configValues) {
        var config = FileConfig.of(configFile);
        config.putAll(toConfig(configValues));
        config.save();
        config.close();
    }

    private Object applyValues(LinkedHashMap<String, Object> configValues, Object thisConfig) {
        for (var field : thisConfig.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
            } catch (InaccessibleObjectException e) {
                continue;
            }

            if (!configValues.containsKey(field.getName())) continue;

            if (field.isAnnotationPresent(Option.class)) {
                try {
                    field.set(thisConfig, configValues.get(field.getName()));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (field.isAnnotationPresent(Category.class)) {
                try {
                    field.set(thisConfig, applyValues((LinkedHashMap<String, Object>) configValues.get(field.getName()), field.get(thisConfig)));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return thisConfig;
    }

}
