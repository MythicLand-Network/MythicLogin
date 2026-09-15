package cn.mythicland.mythiclogin.config;

import cn.mythicland.mythiclogin.config.model.EmailSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class EmailConfig extends AbstractYamlConfiguration {
    private EmailSettings settings;

    public EmailConfig(Plugin plugin) {
        super(plugin, "email.yml");
        reload();
    }

    public void reload() {
        reloadYamlConfiguration();
        settings = parse(getYamlConfiguration());
    }

    public EmailSettings getSettings() {
        return settings;
    }

    static EmailSettings parse(YamlConfiguration config) {
        return new EmailSettings(
                config.getString("resend.api-key", ""),
                config.getString("resend.from", ""),
                config.getInt("verification.code-length", 6),
                config.getInt("verification.expire-minutes", 5),
                config.getInt("verification.resend-cooldown-seconds", 60),
                config.getInt("verification.max-attempts", 5)
        );
    }
}
