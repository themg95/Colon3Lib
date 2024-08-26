package dev.mg95.colon3lib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;

import java.io.File;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    private ObjectMapper mapper = new ObjectMapper(new TomlFactory());


    public void init(Object config, String id) {
        try {
            _init(config, id);
        } catch (Exception e) {
            throw new RuntimeException("There was an error loading/initializing the " + id + " config. Please fix or remove the config file.\nStacktrace:", e);
        }
    }

    private void _init(Object config, String id) throws Exception {
        LinkedHashMap<String, Object> configValues = new LinkedHashMap<>();

        var fields = config.getClass().getDeclaredFields();
        for (var field : fields) {
            if (field.isAnnotationPresent(Option.class)) {
                field.setAccessible(true);
                configValues.put(field.getName(), field.get(config));

            }
        }


        var file = new File("config/" + id + ".toml");


        if (file.exists()) {
            var loadedConfigValues = loadConfig(file);
            overrideConfig(configValues, loadedConfigValues);
        }

        saveConfig(file, configValues);
    }

    private LinkedHashMap<String, Object> overrideConfig(LinkedHashMap<String, Object> configValues, LinkedHashMap<String, Object> loadedConfigValues) {
        for (var entry : loadedConfigValues.entrySet()) {
            if (entry.getValue() instanceof LinkedHashMap) {
                var map = (LinkedHashMap<String, Object>) configValues.get(entry.getKey());
                map.putAll(overrideConfig(new LinkedHashMap<>(), (LinkedHashMap<String, Object>) entry.getValue()));
                configValues.put(entry.getKey(), map);
                continue;
            }
            configValues.put(entry.getKey(), entry.getValue());
        }

        return configValues;
    }

    private LinkedHashMap<String, Object> loadConfig(File configFile) throws IOException {
        return (LinkedHashMap<String, Object>) mapper.readValue(configFile, Map.class);

    }

    private void saveConfig(File configFile, LinkedHashMap<String, Object> configValues) throws IOException {
        mapper.writerFor(Map.class).writeValue(configFile, configValues);
    }
}
