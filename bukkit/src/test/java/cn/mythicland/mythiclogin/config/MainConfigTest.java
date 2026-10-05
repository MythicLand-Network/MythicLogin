package cn.mythicland.mythiclogin.config;

import cn.mythicland.mythiclogin.config.model.PluginSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.*;

class MainConfigTest {

    @Test
    void shouldPersistGeneratedSecretForNewAndExistingConfigurations(@TempDir Path tempDir) throws Exception {
        String[] configurations = {null, "security:\n  auth-timeout-seconds: 45\n", "premium:\n  shared-secret: ''\n"};
        for (int index = 0; index < configurations.length; index++) {
            Path directory = Files.createDirectory(tempDir.resolve("config-" + index));
            Path file = directory.resolve("config.yml");
            if (configurations[index] != null) Files.writeString(file, configurations[index]);
            Plugin plugin = mock(Plugin.class);
            when(plugin.getDataFolder()).thenReturn(directory.toFile());
            doAnswer(invocation -> {
                try (var resource = MainConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
                    Files.copy(resource, file);
                }
                return null;
            }).when(plugin).saveResource("config.yml", false);

            MainConfig config = new MainConfig(plugin);
            String secret = config.getSettings().premium().sharedSecret();
            assertEquals(32, Base64.getUrlDecoder().decode(secret).length);
            assertEquals(secret, YamlConfiguration.loadConfiguration(file.toFile()).getString("premium.shared-secret"));
            config.reload();
            assertEquals(secret, config.getSettings().premium().sharedSecret());
            if (index == 1) assertEquals(45, config.getYamlConfiguration().getInt("security.auth-timeout-seconds"));
        }
    }

    @Test
    void shouldPreserveConfiguredSecret(@TempDir Path tempDir) throws Exception {
        String secret = "abcdefghijklmnopqrstuvwxyz0123456789";
        Files.writeString(tempDir.resolve("config.yml"), "premium:\n  enabled: true\n  shared-secret: " + secret + "\n");
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        assertEquals(secret, new MainConfig(plugin).getSettings().premium().sharedSecret());
    }

    @Test
    void shouldResolveSqliteFileFromPluginDataDirectory(@TempDir Path tempDir) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("database.mode", "sqlite");
        yaml.set("database.sqlite.file", "data/mythiclogin.db");

        PluginSettings settings = MainConfig.parse(yaml, tempDir);

        assertEquals(
                tempDir.resolve("data/mythiclogin.db").toAbsolutePath().normalize(),
                settings.database().sqliteFile()
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
                settings.restrictions().getCommandPrefixWhitelist()
        );
        assertEquals(
                "jdbc:postgresql://127.0.0.1:5432/mythiclogin",
                settings.database().jdbcUrl()
        );
        assertEquals("&7剩余时间: &f{0} &7秒", settings.display().getActionBarLoginText());
    }
}
