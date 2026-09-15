package cn.mythicland.mythiclogin.config.model;

import java.util.regex.Pattern;

public final class EmailSettings {
    private static final String ADDRESS_PATTERN =
            "[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}";
    private static final Pattern FROM_PATTERN = Pattern.compile(
            "^(?:" + ADDRESS_PATTERN + "|[^<>]+\\s<" + ADDRESS_PATTERN + ">)$"
    );

    private final String apiKey;
    private final String from;
    private final int codeLength;
    private final int expireMinutes;
    private final int resendCooldownSeconds;
    private final int maxAttempts;

    public EmailSettings(
            String apiKey,
            String from,
            int codeLength,
            int expireMinutes,
            int resendCooldownSeconds,
            int maxAttempts
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from == null ? "" : from.trim();
        this.codeLength = codeLength;
        this.expireMinutes = expireMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxAttempts = maxAttempts;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getFrom() {
        return from;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public boolean isEnabled() {
        return validationError() == null;
    }

    public String validationError() {
        if (apiKey.isEmpty()) return "resend.api-key is missing";
        if (!apiKey.startsWith("re_")) return "resend.api-key is invalid";
        if (!FROM_PATTERN.matcher(from).matches()) return "resend.from is missing or invalid";
        if (codeLength < 4 || codeLength > 10) return "verification.code-length must be between 4 and 10";
        if (expireMinutes <= 0) return "verification.expire-minutes must be positive";
        if (resendCooldownSeconds < 0) return "verification.resend-cooldown-seconds cannot be negative";
        if (maxAttempts <= 0) return "verification.max-attempts must be positive";
        return null;
    }
}
