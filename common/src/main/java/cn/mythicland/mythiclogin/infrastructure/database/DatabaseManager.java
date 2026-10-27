package cn.mythicland.mythiclogin.infrastructure.database;

import cn.mythicland.framework.database.DatabaseConfiguration;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class DatabaseManager {
    private final Logger logger;
    private final String poolName;
    private volatile cn.mythicland.framework.database.DatabaseManager frameworkDatabase;
    private volatile DatabaseSettings settings;

    public DatabaseManager(Logger logger, DatabaseSettings settings, String poolName) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.poolName = Objects.requireNonNull(poolName, "pool name");
        this.frameworkDatabase = this.createDataSource(settings);
    }

    public DataSource dataSource() {
        return this.frameworkDatabase.dataSource();
    }

    public DatabaseMode mode() {
        return this.settings.mode();
    }

    public void reloadIfNecessary(DatabaseSettings newSettings) {
        Objects.requireNonNull(newSettings, "new settings");
        if (newSettings.equals(this.settings)) return;
        cn.mythicland.framework.database.DatabaseManager replacement = this.createDataSource(newSettings);
        cn.mythicland.framework.database.DatabaseManager previous = this.frameworkDatabase;
        this.settings = newSettings;
        this.frameworkDatabase = replacement;
        previous.close();
    }

    public void shutdown() {
        this.frameworkDatabase.close();
    }

    private cn.mythicland.framework.database.DatabaseManager createDataSource(DatabaseSettings settings) {
        cn.mythicland.framework.database.DatabaseMode mode = settings.mode()
                == cn.mythicland.mythiclogin.infrastructure.database.DatabaseMode.SQLITE
                ? cn.mythicland.framework.database.DatabaseMode.SQLITE
                : cn.mythicland.framework.database.DatabaseMode.POSTGRESQL;
        DatabaseConfiguration.Builder configuration = DatabaseConfiguration.builder(mode, settings.jdbcUrl())
                .sqliteFile(settings.sqliteFile())
                .credentials(settings.username(), settings.password())
                .maximumPoolSize(settings.maximumPoolSize())
                .minimumIdle(1)
                .poolName(this.poolName)
                .migrationLocations(List.of("classpath:db/" + settings.mode().name().toLowerCase()))
                .placeholders(Map.of("table", settings.table()));
        if (settings.mode() == DatabaseMode.SQLITE) {
            configuration.dataSourceProperty("journal_mode", "WAL")
                    .dataSourceProperty("busy_timeout", "30000");
        }
        cn.mythicland.framework.database.DatabaseManager manager = new cn.mythicland.framework.database.DatabaseManager(
                configuration.build(),
                this.logger
        );
        manager.start();
        return manager;
    }
}
