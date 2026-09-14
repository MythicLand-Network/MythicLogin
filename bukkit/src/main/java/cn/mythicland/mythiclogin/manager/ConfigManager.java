package cn.mythicland.mythiclogin.manager;

import cn.mythicland.mythiclogin.config.MainConfig;
import cn.mythicland.mythiclogin.config.MessageConfig;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class ConfigManager {

    private final MainConfig mainConfig;
    private final MessageConfig messageConfig;

    public ConfigManager(Plugin plugin) {
        this.mainConfig = new MainConfig(plugin);
        this.messageConfig = new MessageConfig(plugin);
    }

    public void reload() {
        mainConfig.reload();
        messageConfig.reload();
    }

    public PluginSettings getSettings() {
        return mainConfig.getSettings();
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public YamlConfiguration getMessages() {
        return messageConfig.getYamlConfiguration();
    }
}
