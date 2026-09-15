package cn.mythicland.mythiclogin.email;

public record VerificationEmail(
        VerificationPurpose purpose,
        String recipient,
        String playerName,
        String code,
        int expireMinutes,
        String idempotencyKey
) {
}
