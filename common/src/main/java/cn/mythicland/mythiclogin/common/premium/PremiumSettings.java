package cn.mythicland.mythiclogin.common.premium;

import java.nio.charset.StandardCharsets;

public record PremiumSettings(boolean enabled, String sharedSecret) {
    public PremiumSettings {
        if (enabled && (sharedSecret == null || sharedSecret.getBytes(StandardCharsets.UTF_8).length < 32)) {
            throw new IllegalArgumentException("premium.shared-secret must contain at least 32 bytes");
        }
    }

    public static PremiumSettings disabled() {
        return new PremiumSettings(false, "");
    }
}
