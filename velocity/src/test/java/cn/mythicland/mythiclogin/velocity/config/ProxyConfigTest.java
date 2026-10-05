package cn.mythicland.mythiclogin.velocity.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyConfigTest {
    @Test
    void shouldPersistGeneratedSecretForNewAndExistingConfigurations(@TempDir Path tempDir) throws Exception {
        String[] configurations = {null, "security:\n  session-timeout-minutes: 12\n", "premium:\n  shared-secret: ''\n"};
        for (int index = 0; index < configurations.length; index++) {
            Path directory = Files.createDirectory(tempDir.resolve("config-" + index));
            Path file = directory.resolve("config.yml");
            if (configurations[index] != null) Files.writeString(file, configurations[index]);

            ProxyConfig config = new ProxyConfig(directory, ProxyConfigTest.class.getClassLoader());
            String secret = config.getSettings().getPremium().sharedSecret();
            assertEquals(32, Base64.getUrlDecoder().decode(secret).length);
            String savedText = Files.readString(file);
            assertFalse(savedText.contains("{"));
            assertFalse(savedText.contains("["));
            if (index == 0) assertTrue(savedText.contains("      - /l\n"));
            Map<String, Object> saved = new Yaml().load(savedText);
            assertEquals(secret, ((Map<?, ?>) saved.get("premium")).get("shared-secret"));
            config.reload();
            assertEquals(secret, config.getSettings().getPremium().sharedSecret());
            if (index == 1) assertEquals(12, config.getInt("security.session-timeout-minutes", 5));
        }
    }

    @Test
    void shouldPreserveConfiguredSecret(@TempDir Path tempDir) throws Exception {
        String secret = "abcdefghijklmnopqrstuvwxyz0123456789";
        Files.writeString(tempDir.resolve("config.yml"), "premium:\n  enabled: true\n  shared-secret: " + secret + "\n");
        ProxyConfig config = new ProxyConfig(tempDir, ProxyConfigTest.class.getClassLoader());
        assertEquals(secret, config.getSettings().getPremium().sharedSecret());
    }
}
