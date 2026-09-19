package cn.mythicland.mythiclogin.config;

import cn.mythicland.mythiclogin.config.model.EmailSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailConfigTest {

    @Test
    void shouldLoadConfiguredEmailSettings() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("resend.api-key", "re_test");
        yaml.set("resend.from", "MythicLogin <login@example.com>");
        yaml.set("verification.code-length", 8);
        yaml.set("verification.expire-minutes", 10);
        yaml.set("verification.resend-cooldown-seconds", 30);
        yaml.set("verification.max-attempts", 3);

        EmailSettings settings = EmailConfig.parse(yaml);

        assertTrue(settings.isEnabled());
        assertEquals(8, settings.codeLength());
        assertEquals(10, settings.expireMinutes());
        assertEquals(30, settings.resendCooldownSeconds());
        assertEquals(3, settings.maxAttempts());
    }

    @Test
    void shouldDisableEmailWhenResendCredentialsAreMissing() {
        EmailSettings settings = EmailConfig.parse(new YamlConfiguration());

        assertFalse(settings.isEnabled());
        assertEquals("resend.api-key is missing", settings.validationError());
    }
}
