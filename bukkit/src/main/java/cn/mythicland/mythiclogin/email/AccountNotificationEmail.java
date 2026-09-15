package cn.mythicland.mythiclogin.email;

public record AccountNotificationEmail(
        AccountNotificationType type,
        String recipient,
        String playerName,
        String idempotencyKey
) {
}
