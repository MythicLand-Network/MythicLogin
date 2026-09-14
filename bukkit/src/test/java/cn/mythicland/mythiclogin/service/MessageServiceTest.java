package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.manager.ConfigManager;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageServiceTest {

    @Test
    void shouldOnlyRenderPrefixWhenPlaceholderExists(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.yml"), "", StandardCharsets.UTF_8);
        Files.writeString(
            tempDir.resolve("messages.yml"),
            "prefix: '&7[&dTest&7]&r'\nplain: '&ahello'\nwith-prefix: '{prefix} &ahello'\n",
            StandardCharsets.UTF_8
        );

        MessageService messageService = new MessageService(createConfigManager(tempDir));

        assertEquals("\u00A7ahello", messageService.get("plain"));
        assertEquals("\u00A77[\u00A7dTest\u00A77]\u00A7r \u00A7ahello", messageService.get("with-prefix"));
    }

    private ConfigManager createConfigManager(Path tempDir) {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("MessageServiceTest"));
        return new ConfigManager(plugin);
    }
}
