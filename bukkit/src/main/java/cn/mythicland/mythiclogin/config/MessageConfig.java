package cn.mythicland.mythiclogin.config;

import org.bukkit.plugin.Plugin;

public final class MessageConfig extends AbstractYamlConfiguration {

    public MessageConfig(Plugin plugin) {
        super(plugin, "messages.yml", "message.yml");
    }

    public void reload() {
        reloadYamlConfiguration();
    }
}
