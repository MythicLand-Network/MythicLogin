package cn.mythicland.mythiclogin.manager;

import cn.mythicland.mythiclogin.config.EmailConfig;
import cn.mythicland.mythiclogin.config.MainConfig;
import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {

    private final MainConfig mainConfig;
    private final EmailConfig emailConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.mainConfig = new MainConfig(plugin);
        this.emailConfig = new EmailConfig(plugin);
    }

    public void reload() {
        mainConfig.reload();
        emailConfig.reload();
    }

    public PluginSettings getSettings() {
        return mainConfig.getSettings();
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public EmailSettings getEmailSettings() {
        return emailConfig.getSettings();
    }

}
