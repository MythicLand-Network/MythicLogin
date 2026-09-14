package cn.mythicland.mythiclogin.manager;

import cn.mythicland.mythiclogin.config.MainConfig;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import org.bukkit.plugin.Plugin;

public final class ConfigManager {

    private final MainConfig mainConfig;

    public ConfigManager(Plugin plugin) {
        this.mainConfig = new MainConfig(plugin);
    }

    public void reload() {
        mainConfig.reload();
    }

    public PluginSettings getSettings() {
        return mainConfig.getSettings();
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

}
