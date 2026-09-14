package cn.mythicland.mythiclogin.infrastructure.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseSettingsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldBuildSqliteJdbcUrl() {
        Path databaseFile = this.temporaryDirectory.resolve("accounts.db");
        DatabaseSettings settings = DatabaseSettings.sqlite(databaseFile, "accounts");

        assertEquals(DatabaseMode.SQLITE, settings.mode());
        assertEquals("jdbc:sqlite:" + databaseFile.toAbsolutePath(), settings.jdbcUrl());
        assertEquals(1, settings.maximumPoolSize());
    }

    @Test
    void shouldBuildPostgresqlJdbcUrl() {
        DatabaseSettings settings = DatabaseSettings.postgresql(
                "127.0.0.1",
                5432,
                "mythiclogin",
                "postgres",
                "change-me",
                "accounts"
        );

        assertEquals(DatabaseMode.POSTGRESQL, settings.mode());
        assertEquals("jdbc:postgresql://127.0.0.1:5432/mythiclogin", settings.jdbcUrl());
        assertEquals(10, settings.maximumPoolSize());
    }
}
