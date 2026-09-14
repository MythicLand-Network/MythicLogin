package cn.mythicland.mythiclogin.velocity.config;

import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseMode;
import cn.mythicland.mythiclogin.common.restriction.CommandAccessMode;
import cn.mythicland.mythiclogin.common.restriction.ProxyCommandSettings;

import java.nio.file.Path;

public final class ProxyConfig extends AbstractYamlConfiguration {

    private PluginSettings settings;

    public ProxyConfig(Path dataDirectory, ClassLoader classLoader) {
        super(dataDirectory, classLoader, "config.yml");
        reload();
    }

    public void reload() {
        reloadYamlConfiguration();
        settings = parse();
    }

    public PluginSettings getSettings() {
        return settings;
    }

    private PluginSettings parse() {
        return new PluginSettings(
            proxyDatabaseSettings(
                DatabaseMode.parse(getString("database.mode", "postgresql")),
                getString("database.host", "127.0.0.1"),
                getInt("database.port", 5432),
                getString("database.name", "mythiclogin"),
                getString("database.username", "root"),
                getString("database.password", ""),
                getString("database.table", DatabaseSettings.TABLE_NAME)
            ),
            new SessionSettings(
                getBoolean("security.session-enabled", true),
                getInt("security.session-timeout-minutes", 5)
            ),
            new LoginServerSettings(
                getBoolean("restrictions.login-servers.all-servers-login", true),
                getStringList("restrictions.login-servers.names")
            ),
            new ProxyCommandSettings(
                getBoolean("restrictions.proxy-commands.enabled", true),
                CommandAccessMode.fromString(
                    getString("restrictions.proxy-commands.mode", CommandAccessMode.WHITELIST.name()),
                    CommandAccessMode.WHITELIST
                ),
                getStringList("restrictions.proxy-commands.whitelist"),
                getStringList("restrictions.proxy-commands.blacklist")
            )
        );
    }

    private DatabaseSettings proxyDatabaseSettings(
            DatabaseMode mode,
            String host,
            int port,
            String name,
            String username,
            String password,
            String table
    ) {
        if (mode != DatabaseMode.POSTGRESQL) {
            throw new IllegalArgumentException("Velocity deployments require database.mode=postgresql");
        }
        return new DatabaseSettings(mode, Path.of("mythiclogin.db"), host, port, name, username, password, table);
    }
}
