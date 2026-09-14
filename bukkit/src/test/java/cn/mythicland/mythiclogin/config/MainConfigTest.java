package cn.mythicland.mythiclogin.config;

import cn.mythicland.mythiclogin.config.model.PluginSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class MainConfigTest {

    @Test
    void shouldResolveSqliteFileFromPluginDataDirectory(@TempDir Path tempDir) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("database.mode", "sqlite");
        yaml.set("database.sqlite.file", "data/mythiclogin.db");

        PluginSettings settings = MainConfig.parse(yaml, tempDir);

        assertEquals(
                tempDir.resolve("data/mythiclogin.db").toAbsolutePath().normalize(),
                settings.getDatabase().sqliteFile()
        );
    }

    @Test
    void shouldNormalizeWhitelistAndLoadDatabaseSettings(@TempDir Path tempDir) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("database.mode", "postgresql");
        yaml.set("database.postgresql.host", "127.0.0.1");
        yaml.set("database.postgresql.port", 5432);
        yaml.set("database.postgresql.name", "mythiclogin");
        yaml.set("database.postgresql.username", "root");
        yaml.set("database.postgresql.password", "change-me");
        yaml.set("database.table", "mythiclogin");
        yaml.set("security.session-enabled", true);
        yaml.set("security.session-timeout-minutes", 5);
        yaml.set("security.auth-timeout-seconds", 60);
        yaml.set("security.min-password-length", 4);
        yaml.set("security.max-password-length", 26);
        yaml.set("restrictions.block-non-whitelist-tab-complete", true);
        yaml.set("restrictions.command-prefix-whitelist", Arrays.asList("login", " /Reg ", "", null));

        PluginSettings settings = MainConfig.parse(yaml, tempDir);
        assertIterableEquals(
                Arrays.asList("/login", "/reg"),
                settings.getRestrictions().getCommandPrefixWhitelist()
        );
        assertEquals(
                "jdbc:postgresql://127.0.0.1:5432/mythiclogin",
                settings.getDatabase().jdbcUrl()
        );
        assertEquals("&7剩余时间: &f{0} &7秒", settings.getDisplay().getActionBarLoginText());
    }
}
