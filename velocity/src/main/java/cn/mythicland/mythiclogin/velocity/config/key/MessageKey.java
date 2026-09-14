package cn.mythicland.mythiclogin.velocity.config.key;

public enum MessageKey {
    PREFIX("&7[&dMythicLoginVelocity&7]&r"),
    PROXY_CASE_MISMATCH_KICK("&c检测到已有账号, 请使用正确的游戏账户 &f{0} &c登录。"),
    PROXY_COMMAND_BLOCKED("&c你需要先完成登录或注册。"),
    PROXY_ACCOUNT_DATA_MISSING("&c代理端没有找到你的账号数据, 请联系管理员检查 MythicLoginVelocity 数据库配置。");

    private final String template;

    MessageKey(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }
}
