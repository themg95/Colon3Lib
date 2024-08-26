package dev.mg95.colon3lib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;

import java.io.File;

import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    ObjectMapper mapper = new ObjectMapper(new TomlFactory());

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
            var loadedConfigValues = (LinkedHashMap<String, Object>) mapper.readValue(file, Map.class);

            overrideConfig(configValues, loadedConfigValues);

        }
        mapper.writerFor(Map.class).writeValue(file, configValues);


    }

    private LinkedHashMap<String, Object> overrideConfig(LinkedHashMap<String, Object> configValues, LinkedHashMap<String, Object> loadedConfigValues) {
        for (var entry : loadedConfigValues.entrySet()) {
            if (entry.getValue() instanceof LinkedHashMap) {
                var obj = mapper.convertValue(configValues, configValues.getClass());
                var map = (LinkedHashMap<String, Object>) obj.get(entry.getKey());
                map.putAll(overrideConfig(new LinkedHashMap<>(), (LinkedHashMap<String, Object>) entry.getValue()));
                configValues.put(entry.getKey(), map);
                continue;
            }
            configValues.put(entry.getKey(), entry.getValue());
        }

        return configValues;
    }
}
