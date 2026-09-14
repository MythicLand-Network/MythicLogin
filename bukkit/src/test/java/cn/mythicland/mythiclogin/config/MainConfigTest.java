package cn.mythicland.mythiclogin.config;

import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.service.MessageService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MainConfigTest {

    @Test
    void shouldNormalizeWhitelistAndLoadMessageFallback(@TempDir Path tempDir) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("database.host", "127.0.0.1");
        yaml.set("database.port", 3306);
        yaml.set("database.name", "mythiclogin");
        yaml.set("database.username", "root");
        yaml.set("database.password", "change-me");
        yaml.set("database.table", "mythiclogin");
        yaml.set("security.session-enabled", true);
        yaml.set("security.session-timeout-minutes", 5);
        yaml.set("security.auth-timeout-seconds", 60);
        yaml.set("security.min-password-length", 4);
        yaml.set("security.max-password-length", 26);
        yaml.set("restrictions.block-non-whitelist-tab-complete", true);
        yaml.set("restrictions.command-prefix-whitelist", Arrays.asList("login", " /Reg ", "", null));
        yaml.set("display.action-bar.login-text", "&7L {0}");
        yaml.set("display.action-bar.register-text", "&7R {0}");

        assertIterableEquals(Arrays.asList("/login", "/reg"), MainConfig.parse(yaml).getRestrictions().getCommandPrefixWhitelist());

        Files.writeString(tempDir.resolve("config.yml"), "", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("messages.yml"), "prefix: '&7[&dMythicLogin&7]&r'\n", StandardCharsets.UTF_8);

        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("MainConfigTest"));

        ConfigManager configManager = new ConfigManager(plugin);
        MessageService messageService = new MessageService(configManager);
        assertEquals("missing.path", messageService.get("missing.path"));
    }
}
