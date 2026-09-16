package cn.mythicland.mythiclogin.config;

import org.bukkit.plugin.Plugin;

public abstract class AbstractYamlConfiguration
        extends cn.mythicland.framework.bukkit.config.AbstractYamlConfiguration {
    protected AbstractYamlConfiguration(
            Plugin plugin,
            String fileName,
            String... legacyFileNames
    ) {
        super(plugin, fileName, legacyFileNames);
    }
}
