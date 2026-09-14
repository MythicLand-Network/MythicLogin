package cn.mythicland.mythiclogin.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public abstract class AbstractYamlConfiguration {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration yamlConfiguration;

    protected AbstractYamlConfiguration(Plugin plugin, String fileName, String... legacyFileNames) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        saveDefaultConfig(fileName, legacyFileNames);
        loadYamlConfiguration();
    }

    private void saveDefaultConfig(String fileName, String... legacyFileNames) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create plugin data folder");
        }
        if (file.exists()) {
            return;
        }
        if (tryMigrateLegacyFile(legacyFileNames)) {
            return;
        }
        plugin.saveResource(fileName, false);
    }

    private boolean tryMigrateLegacyFile(String... legacyFileNames) {
        for (String legacyFileName : legacyFileNames) {
            if (legacyFileName == null || legacyFileName.trim().isEmpty()) {
                continue;
            }
            File legacyFile = new File(plugin.getDataFolder(), legacyFileName);
            if (!legacyFile.exists()) {
                continue;
            }
            try {
                Files.copy(legacyFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Migrated legacy file " + legacyFileName + " to " + file.getName() + ".");
                return true;
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to migrate " + legacyFileName + " to " + file.getName(), exception);
            }
        }
        return false;
    }

    public void loadYamlConfiguration() {
        yamlConfiguration = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadYamlConfiguration() {
        loadYamlConfiguration();
    }

    public YamlConfiguration getYamlConfiguration() {
        return yamlConfiguration;
    }
}
