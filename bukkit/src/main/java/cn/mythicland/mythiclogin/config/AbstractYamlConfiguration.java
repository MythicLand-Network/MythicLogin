package cn.mythicland.mythiclogin.config;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractYamlConfiguration
        extends cn.mythicland.framework.bukkit.config.AbstractYamlConfig {
    protected AbstractYamlConfiguration(
            JavaPlugin plugin,
            String fileName
    ) {
        super(plugin, fileName);
    }
}
