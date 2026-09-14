package cn.mythicland.mythiclogin.config.key;

public enum MessageKey {
    PREFIX("&7[&dMythicLogin&7]&r"),
    SYSTEM_CONFIG_RELOADED("&a配置已重载。"),
    SYSTEM_NO_PERMISSION("&c你没有权限执行这个命令。"),
    SYSTEM_PLAYER_ONLY("&c该命令只能由玩家执行。"),
    SYSTEM_LOADING("&c账号数据仍在加载, 请稍后再试。"),
    SYSTEM_STARTING_UP_KICK("&c服务器仍在启动中, 请稍后重新加入。"),
    SYSTEM_TIMEOUT_KICK("&c登录超时, 请重新加入服务器。"),
    AUTH_COMMAND_BLOCKED("&c你需要先完成登录或注册。"),
    AUTH_LOGIN_REQUIRED("&c使用 /login <密码> 完成登录。"),
    AUTH_REGISTER_REQUIRED("&c使用 /register <密码> <重复密码> 完成注册"),
    AUTH_PASSWORD_MISMATCH("&c两次输入的密码不一致。"),
    AUTH_PASSWORD_LENGTH("密码长度需要在 &f{0}&7 - &f{1} &c个字符之间。"),
    AUTH_ALREADY_AUTHENTICATED("&c你已经登录过了!"),
    AUTH_ALREADY_REGISTERED("&c你已经注册过了!"),
    AUTH_NOT_REGISTERED("&c你还没有注册!"),
    AUTH_CASE_MISMATCH("&c检测到已有账号, 请使用正确的游戏账户 &f{0} &c登录。"),
    AUTH_NOT_AUTHENTICATED("&c你当前还没有登录。"),
    AUTH_CHANGE_PASSWORD_USAGE("&c使用 /cp <新密码> <重复新密码> 修改你的密码。"),
    AUTH_PASSWORD_CHANGED("&a密码修改成功。"),
    AUTH_PASSWORD_CHANGE_FAILED("&c修改密码失败, 请查看控制台。"),
    AUTH_LOGIN_SUCCESS("&a登录成功, 欢迎回来。"),
    AUTH_REGISTER_SUCCESS("&a注册成功, 已为您自动登录。"),
    AUTH_AUTO_LOGIN_MESSAGE("&a欢迎回来, 已为你自动登录。"),
    AUTH_LOGOUT_SUCCESS("&c你已成功登出, 请重新登录。"),
    AUTH_LOGIN_FAILED("&c密码错误。"),
    RESTRICT_CHAT_BLOCKED("&c你现在不能发言。"),
    RESTRICT_INTERACTION_BLOCKED("&c你现在不能这样做。"),
    ADMIN_ACCOUNT_MISSING("&c找不到玩家 &f{0} &c的账号。"),
    ADMIN_PASSWORD_UPDATED("&c已重置玩家 &f{0} &c的密码。"),
    ADMIN_PASSWORD_UPDATE_FAILED("&c修改密码失败, 请查看控制台。"),
    ADMIN_UNREGISTER_SUCCESS("&a已注销玩家 &f{0} &a的账号。"),
    ADMIN_UNREGISTER_FAILED("&c注销账号失败, 请查看控制台。");

    private final String template;

    MessageKey(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }
}
