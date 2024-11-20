package dev.mg95.colon3lib.config.v2;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigWrapper<T> {
    private Object config;
    private String id;
    private Class<T> modelClazz;
    private Object outer;
    private String path;

    protected void init(Object config, String id, Class<T> modelClazz) {
        this.config = config;
        this.id = id;
        this.modelClazz = modelClazz;
        this.path = "config/" + this.id + ".toml";

        _init();
    }

    protected void init(Object config, Class<T> modelClazz, Object outer) {
        this.config = config;
        this.modelClazz = modelClazz;
        this.outer = outer;

        _init();
    }

    private void _init() {
        Object object;
        try {
            object = modelClazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (Field field : this.config.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;

            try {
                field.setAccessible(true);
                field.set(null, modelClazz.getField(field.getName()).get(object));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void save() {
        if (outer instanceof ConfigWrapper<?> outerConfig) {
            outerConfig.save();
            return;
        }
        try {
            _save();
        } catch (IllegalAccessException | IOException e) {
            throw new RuntimeException("Failed to save config " + this.id + ". Stacktrace: " + e);
        }
    }

    private void _save() throws IllegalAccessException, IOException {
        var map = getMap(this.config);
        var nc = (Config) ObjectSerializer.standard().serialize(map, Config::inMemory);

        var path = Paths.get("config");
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        var writer = TomlFormat.instance().createWriter();
        writer.setWriteTableInlinePredicate(table -> true);
        writer.write(nc, new File(this.path), WritingMode.REPLACE);
    }

    public void load() {
        if (outer instanceof ConfigWrapper<?> outerConfig) {
            outerConfig.load();
            return;
        }
        try {
            _load();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Failed to load config " + this.id + ". Stacktrace: " + e);
        }
    }

    private void _load() throws IllegalAccessException, NoSuchFieldException {
        var map = getMap(this.config);
        var file = new File(path);
        if (!file.exists()) return;
        var nc = FileConfig.of(file);
        nc.load();

        var loadedMap = configToMap(nc);
        var merged = mergeMaps(map, loadedMap);
        mapToObject(config, merged);
    }

    public void reset(String fieldName, @Nullable String parent) {
        try {
            _reset(fieldName, parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void _reset(String fieldName, @Nullable String parent) throws Exception {
        if (parent == null) {
            var field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, this.modelClazz.getDeclaredField(fieldName).get(modelClazz.getDeclaredConstructor().newInstance()));
        } else {
            Class<?> parentClazz = modelClazz;
            for (var parentField : parent.split("\\.")) {
                parentClazz = Arrays.stream(parentClazz.getDeclaredClasses())
                        .filter(c -> c.getSimpleName().equals(parentField))
                        .findFirst()
                        .orElse(null);
            }
            var modelField = parentClazz.getDeclaredField(fieldName);
            modelField.setAccessible(true);

            Object parentObject = this.config;
            for (var parentField : parent.split("\\.")) {
                parentObject = parentObject.getClass().getDeclaredField(parentField).get(parentObject);
            }
            var field = parentObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, modelField.get(parentClazz.getDeclaredConstructor().newInstance()));
        }


    }

    private static LinkedHashMap<String, Object> getMap(Object config) throws IllegalAccessException {
        var map = new LinkedHashMap<String, Object>();
        for (Field field : config.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Exclude.class)) continue;
            field.setAccessible(true);
            if (field.isAnnotationPresent(Nested.class)) {
                map.put(field.getName(), getMap(field.get(config)));
            } else {
                map.put(field.getName(), field.get(config));
            }
        }

        return map;
    }

    private static LinkedHashMap<String, Object> configToMap(Config config) throws IllegalAccessException {
        var map = new LinkedHashMap<String, Object>();
        for (Config.Entry entry : config.entrySet()) {
            if (entry.getValue() instanceof Config) {
                map.put(entry.getKey(), configToMap(entry.getValue()));
            } else {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return map;
    }

    private static LinkedHashMap<String, Object> mergeMaps(LinkedHashMap<String, Object> base, LinkedHashMap<String, Object> loaded) throws IllegalAccessException {
        for (Object entryObject : loaded.entrySet()) {
            var entry = (Map.Entry<String, Object>) entryObject;
            if (!base.containsKey(entry.getKey())) continue;

            if (entry.getValue() instanceof Map<?, ?>) {
                base.put(entry.getKey(), mergeMaps((LinkedHashMap<String, Object>) base.get(entry.getKey()), (LinkedHashMap<String, Object>) entry.getValue()));
            } else {
                base.put(entry.getKey(), entry.getValue());
            }
        }

        return base;
    }

    private void mapToObject(Object object, LinkedHashMap<String, Object> in) throws NoSuchFieldException, IllegalAccessException {
        var clazz = object.getClass();
        for (Object entryObject : in.entrySet()) {
            var entry = (Map.Entry<String, Object>) entryObject;

            var field = clazz.getDeclaredField(entry.getKey());
            field.setAccessible(true);

            if (entry.getValue() instanceof LinkedHashMap<?, ?>) {
                mapToObject(clazz.getDeclaredField(entry.getKey()).get(object), (LinkedHashMap<String, Object>) entry.getValue());
            } else {
                field.set(object, entry.getValue());
            }
        }
    }
}
