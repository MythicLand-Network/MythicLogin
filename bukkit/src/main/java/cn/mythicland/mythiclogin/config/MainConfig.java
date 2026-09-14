package cn.mythicland.mythiclogin.config;

import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseMode;
import cn.mythicland.mythiclogin.config.model.DisplaySettings;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import cn.mythicland.mythiclogin.config.model.RestrictionSettings;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.file.Path;

public final class MainConfig extends AbstractYamlConfiguration {

    private PluginSettings settings;

    public MainConfig(Plugin plugin) {
        super(plugin, "config.yml");
        reload();
    }

    public void reload() {
        reloadYamlConfiguration();
        settings = parse(getYamlConfiguration());
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public static PluginSettings parse(YamlConfiguration config) {
        return new PluginSettings(
            new DatabaseSettings(
                DatabaseMode.parse(config.getString("database.mode", "sqlite")),
                Path.of(config.getString("database.sqlite-file", "plugins/MythicLogin/mythiclogin.db")),
                config.getString("database.host", "127.0.0.1"),
                config.getInt("database.port", 5432),
                config.getString("database.name", "mythiclogin"),
                config.getString("database.username", "root"),
                config.getString("database.password", ""),
                config.getString("database.table", DatabaseSettings.TABLE_NAME)
            ),
            new SessionSettings(
                config.getBoolean("security.session-enabled", true),
                config.getInt("security.session-timeout-minutes", 5)
            ),
            new RestrictionSettings(
                normalizePrefixes(config.getStringList("restrictions.command-prefix-whitelist")),
                config.getBoolean("restrictions.block-non-whitelist-tab-complete", true),
                true,
                true,
                config.getInt("security.auth-timeout-seconds", 60),
                config.getInt("security.min-password-length", 4),
                config.getInt("security.max-password-length", 26),
                true
            ),
            DisplaySettings.fromConfig(config)
        );
    }

    private static List<String> normalizePrefixes(List<String> prefixes) {
        List<String> normalized = new ArrayList<>();
        for (String prefix : prefixes) {
            if (prefix == null) {
                continue;
            }
            String value = prefix.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) {
                continue;
            }
            if (!value.startsWith("/")) {
                value = "/" + value;
            }
            normalized.add(value);
        }
        return normalized;
    }
}
