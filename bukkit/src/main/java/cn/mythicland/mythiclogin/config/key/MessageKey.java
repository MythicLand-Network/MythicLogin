package cn.mythicland.mythiclogin.config.key;

public enum MessageKey {
    PREFIX("prefix"),
    SYSTEM_CONFIG_RELOADED("system.config-reloaded"),
    SYSTEM_LOADING("system.loading"),
    SYSTEM_STARTING_UP_KICK("system.starting-up-kick"),
    SYSTEM_TIMEOUT_KICK("system.timeout-kick"),
    AUTH_COMMAND_BLOCKED("auth.command-blocked"),
    AUTH_LOGIN_REQUIRED("auth.login-required"),
    AUTH_REGISTER_REQUIRED("auth.register-required"),
    AUTH_PASSWORD_MISMATCH("auth.password-mismatch"),
    AUTH_PASSWORD_LENGTH("auth.password-length"),
    AUTH_ALREADY_AUTHENTICATED("auth.already-authenticated"),
    AUTH_ALREADY_REGISTERED("auth.already-registered"),
    AUTH_NOT_REGISTERED("auth.not-registered"),
    AUTH_CASE_MISMATCH("auth.case-mismatch"),
    AUTH_NOT_AUTHENTICATED("auth.not-authenticated"),
    AUTH_CHANGE_PASSWORD_USAGE("auth.change-password-usage"),
    AUTH_PASSWORD_CHANGED("auth.password-changed"),
    AUTH_PASSWORD_CHANGE_FAILED("auth.password-change-failed"),
    AUTH_LOGIN_SUCCESS("auth.login-success"),
    AUTH_REGISTER_SUCCESS("auth.register-success"),
    AUTH_AUTO_LOGIN_MESSAGE("auth.auto-login-message"),
    AUTH_LOGOUT_SUCCESS("auth.logout-success"),
    AUTH_LOGIN_FAILED("auth.login-failed"),
    RESTRICT_CHAT_BLOCKED("restrict.chat-blocked"),
    RESTRICT_INTERACTION_BLOCKED("restrict.interaction-blocked"),
    ADMIN_ACCOUNT_MISSING("admin.account-missing"),
    ADMIN_PASSWORD_UPDATED("admin.password-updated"),
    ADMIN_PASSWORD_UPDATE_FAILED("admin.password-update-failed"),
    ADMIN_UNREGISTER_SUCCESS("admin.unregister-success"),
    ADMIN_UNREGISTER_FAILED("admin.unregister-failed");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
