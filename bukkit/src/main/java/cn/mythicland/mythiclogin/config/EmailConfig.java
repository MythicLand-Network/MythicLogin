package cn.mythicland.mythiclogin.config;

import cn.mythicland.mythiclogin.config.model.EmailSettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class EmailConfig extends AbstractYamlConfiguration {
    private EmailSettings settings;

    public EmailConfig(JavaPlugin plugin) {
        super(plugin, "email.yml");
        reload();
    }

    public void reload() {
        load();
    }

    @Override
    protected void checkConfig() {
        settings = parse(configuration());
    }

    public EmailSettings getSettings() {
        return settings;
    }

    static EmailSettings parse(FileConfiguration config) {
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
