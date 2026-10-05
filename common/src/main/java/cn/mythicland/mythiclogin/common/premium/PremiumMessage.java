package cn.mythicland.mythiclogin.common.premium;

import java.util.UUID;

public record PremiumMessage(
        Type type,
        UUID nonce,
        String playerName,
        UUID gameUuid,
        UUID premiumUuid,
        boolean showTitle,
        long registerDate,
        String detail
) {
    public enum Type {
        QUERY, OFFLINE, NEW_ACCOUNT, AUTO_LOGIN, BOUND,
        BIND_REQUEST, BIND_ACCEPTED, BIND_REJECTED, BIND_CHECK, PASSWORD_FAILED, COMMAND_FAILED,
        UNBIND_CHECK, UNBIND_REQUEST, UNBOUND, UNBIND_REJECTED
    }
}
