package cn.mythicland.mythiclogin.config;

import cn.mythicland.framework.security.SignedPayload;
import cn.mythicland.mythiclogin.common.premium.PremiumSettings;
import cn.mythicland.mythiclogin.config.model.DisplaySettings;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import cn.mythicland.mythiclogin.config.model.RestrictionSettings;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseMode;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainConfig extends AbstractYamlConfiguration {

    private final Path dataDirectory;
    private PluginSettings settings;

    public MainConfig(JavaPlugin plugin) {
        super(plugin, "config.yml");
        this.dataDirectory = plugin.getDataFolder().toPath();
        reload();
    }

    public void reload() {
        load();
    }

    @Override
    protected void checkConfig() {
        if (configuration().getString("premium.shared-secret", "").isBlank()) {
            configuration().set("premium.shared-secret", SignedPayload.generateSecret());
            markChanged();
        }
        settings = parse(configuration(), dataDirectory);
    }

    public PluginSettings getSettings() {
        return settings;
    }

    static PluginSettings parse(FileConfiguration config, Path dataDirectory) {
        DatabaseMode databaseMode = DatabaseMode.parse(config.getString("database.mode", "sqlite"));
        String table = config.getString("database.table", DatabaseSettings.TABLE_NAME);
        DatabaseSettings databaseSettings = switch (databaseMode) {
            case SQLITE -> DatabaseSettings.sqlite(
                    resolveSqliteFile(
                            dataDirectory,
                            config.getString("database.sqlite.file", "data/mythiclogin.db")
                    ),
                    table
            );
            case POSTGRESQL -> DatabaseSettings.postgresql(
                    config.getString("database.postgresql.host", "127.0.0.1"),
                    config.getInt("database.postgresql.port", 5432),
                    config.getString("database.postgresql.name", "mythiclogin"),
                    config.getString("database.postgresql.username", "root"),
                    config.getString("database.postgresql.password", ""),
                    table
            );
        };
        return new PluginSettings(
                databaseSettings,
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
                DisplaySettings.defaults(),
                new PremiumSettings(config.getBoolean("premium.enabled", false), config.getString("premium.shared-secret", ""))
        );
    }

    private static Path resolveSqliteFile(Path dataDirectory, String configuredFile) {
        Path file = Path.of(configuredFile);
        if (file.isAbsolute()) {
            return file;
        }
        return dataDirectory.resolve(file);
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
