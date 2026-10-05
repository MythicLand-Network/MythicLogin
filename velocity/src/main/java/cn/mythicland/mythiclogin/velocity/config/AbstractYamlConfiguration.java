package cn.mythicland.mythiclogin.velocity.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractYamlConfiguration {

    private final ClassLoader classLoader;
    private final Path file;
    private Map<String, Object> configuration;

    protected AbstractYamlConfiguration(Path dataDirectory, ClassLoader classLoader, String fileName, String... legacyFileNames) {
        this.classLoader = classLoader;
        this.file = dataDirectory.resolve(fileName);
        saveDefaultConfig(dataDirectory, fileName, legacyFileNames);
        loadYamlConfiguration();
    }

    private void saveDefaultConfig(Path dataDirectory, String fileName, String... legacyFileNames) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create plugin data folder", exception);
        }
        if (Files.exists(file)) {
            return;
        }
        if (tryMigrateLegacyFile(dataDirectory, legacyFileNames)) {
            return;
        }
        try (InputStream inputStream = classLoader.getResourceAsStream("velocity/" + fileName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bundled resource " + fileName);
            }
            Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save " + fileName, exception);
        }
    }

    private boolean tryMigrateLegacyFile(Path dataDirectory, String... legacyFileNames) {
        for (String legacyFileName : legacyFileNames) {
            if (legacyFileName == null || legacyFileName.trim().isEmpty()) {
                continue;
            }
            Path legacyFile = dataDirectory.resolve(legacyFileName);
            if (!Files.exists(legacyFile)) {
                continue;
            }
            try {
                Files.copy(legacyFile, file, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to migrate " + legacyFileName + " to " + file.getFileName(), exception);
            }
        }
        return false;
    }

    public void loadYamlConfiguration() {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            configuration = castMap(loaded);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load " + file.getFileName(), exception);
        }
    }

    public void reloadYamlConfiguration() {
        loadYamlConfiguration();
    }

    @SuppressWarnings("unchecked")
    protected final void saveString(String path, String value) {
        String[] segments = path.split("\\.");
        Map<String, Object> section = configuration;
        for (int index = 0; index < segments.length - 1; index++) {
            Object child = section.get(segments[index]);
            if (child == null) {
                Map<String, Object> created = new LinkedHashMap<>();
                section.put(segments[index], created);
                section = created;
            } else if (child instanceof Map<?, ?>) {
                section = (Map<String, Object>) child;
            } else {
                throw new IllegalArgumentException("Invalid configuration section for " + path);
            }
        }
        section.put(segments[segments.length - 1], value);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setIndent(2);
            options.setIndicatorIndent(2);
            options.setIndentWithIndicator(true);
            new Yaml(options).dump(configuration, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save " + file.getFileName(), exception);
        }
    }

    public String getString(String path, String defaultValue) {
        Object value = getValue(path);
        return value == null ? defaultValue : String.valueOf(value);
    }

    public int getInt(String path, int defaultValue) {
        Object value = getValue(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = getValue(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    public List<String> getStringList(String path) {
        Object value = getValue(path);
        if (!(value instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (Object entry : (List<?>) value) {
            if (entry == null) {
                continue;
            }
            values.add(String.valueOf(entry));
        }
        return values;
    }

    private Object getValue(String path) {
        String[] segments = path.split("\\.");
        Object current = configuration;
        for (String segment : segments) {
            if (!(current instanceof Map<?, ?>)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object loaded) {
        if (!(loaded instanceof Map<?, ?>)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) loaded).entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                result.put(String.valueOf(entry.getKey()), castMap(value));
                continue;
            }
            if (value instanceof List<?>) {
                result.put(String.valueOf(entry.getKey()), new ArrayList<>((List<Object>) value));
                continue;
            }
            result.put(String.valueOf(entry.getKey()), value);
        }
        return result;
    }
}
