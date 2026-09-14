package cn.mythicland.mythiclogin.velocity.config.key;

public enum MessageKey {
    PREFIX("prefix"),
    PROXY_CASE_MISMATCH_KICK("proxy.case-mismatch-kick"),
    PROXY_COMMAND_BLOCKED("proxy.command-blocked"),
    PROXY_ACCOUNT_DATA_MISSING("proxy.account-data-missing");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
