package cn.mythicland.mythiclogin.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class DatabaseManager {
    private final Logger logger;
    private final String poolName;
    private volatile HikariDataSource dataSource;
    private volatile DatabaseSettings settings;

    public DatabaseManager(Logger logger, DatabaseSettings settings, String poolName) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.poolName = Objects.requireNonNull(poolName, "pool name");
        this.dataSource = this.createDataSource(settings);
    }

    public DataSource dataSource() {
        return this.dataSource;
    }

    public DatabaseMode mode() {
        return this.settings.mode();
    }

    public void reloadIfNecessary(DatabaseSettings newSettings) {
        Objects.requireNonNull(newSettings, "new settings");
        if (newSettings.equals(this.settings)) return;
        HikariDataSource replacement = this.createDataSource(newSettings);
        HikariDataSource previous = this.dataSource;
        this.settings = newSettings;
        this.dataSource = replacement;
        this.close(previous);
    }

    public void shutdown() {
        this.close(this.dataSource);
    }

    private void close(HikariDataSource source) {
        if (source == null || source.isClosed()) return;
        source.close();
        this.logger.info(this.poolName + " database pool closed.");
    }

    private HikariDataSource createDataSource(DatabaseSettings settings) {
        this.prepareSqliteDirectory(settings);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setMaximumPoolSize(settings.maximumPoolSize());
        config.setPoolName(this.poolName);
        if (settings.mode() == DatabaseMode.POSTGRESQL) {
            config.setUsername(settings.username());
            config.setPassword(settings.password());
        } else {
            config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        }
        HikariDataSource candidate = new HikariDataSource(config);
        try {
            Flyway.configure()
                    .dataSource(candidate)
                    .locations("classpath:db/" + settings.mode().name().toLowerCase())
                    .placeholders(Map.of("table", settings.table()))
                    .load()
                    .migrate();
            return candidate;
        } catch (RuntimeException exception) {
            candidate.close();
            throw new IllegalStateException("Failed to migrate " + settings.mode() + " database", exception);
        }
    }

    private void prepareSqliteDirectory(DatabaseSettings settings) {
        if (settings.mode() != DatabaseMode.SQLITE) return;
        try {
            PathSupport.createParentDirectories(settings.sqliteFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create SQLite database directory", exception);
        }
    }

    private static final class PathSupport {
        private static void createParentDirectories(java.nio.file.Path file) throws IOException {
            java.nio.file.Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
        }
    }
}
