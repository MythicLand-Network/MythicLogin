package cn.mythicland.mythiclogin.config.key;

public enum MessageKey {
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
    AUTH_PASSWORD_LENGTH("&c密码长度需要在 {0} - {1} 个字符之间。"),
    AUTH_ALREADY_AUTHENTICATED("&c你已经登录过了!"),
    AUTH_ALREADY_REGISTERED("&c你已经注册过了!"),
    AUTH_NOT_REGISTERED("&c你还没有注册!"),
    AUTH_CASE_MISMATCH("&c检测到已有账号, 请使用正确的游戏账户 {0} 登录。"),
    AUTH_NOT_AUTHENTICATED("&c你当前还没有登录。"),
    AUTH_CHANGE_PASSWORD_USAGE("&c使用 /cp <新密码> <重复新密码> 修改你的密码。"),
    AUTH_PASSWORD_CHANGED("&a密码修改成功。"),
    AUTH_PASSWORD_CHANGE_FAILED("&c修改密码失败, 请查看控制台。"),
    AUTH_LOGIN_SUCCESS("&a登录成功, 欢迎回来。"),
    AUTH_REGISTER_SUCCESS("&a注册成功, 已为您自动登录。"),
    AUTH_AUTO_LOGIN_MESSAGE("&a欢迎回来, 已为你自动登录。"),
    AUTH_LOGOUT_SUCCESS("&c你已成功登出, 请重新登录。"),
    AUTH_LOGIN_RATE_LIMITED("&c登录尝试过于频繁, 请稍后再试。"),
    AUTH_LOGIN_FAILED("&c密码不正确, 请检查后重新输入。"),
    AUTH_LOGIN_FAILED_WITH_RECOVERY("&c密码不正确。若忘记密码, 请使用 /fp 通过已绑定邮箱重置密码。"),
    AUTH_EMAIL_BIND_RECOMMENDED("&e你的账号尚未绑定邮箱。推荐使用 /bind email <邮箱> 完成绑定, 以便忘记密码时找回账号。"),
    EMAIL_SERVICE_UNAVAILABLE("&c邮箱服务当前不可用, 请联系管理员。"),
    EMAIL_ADDRESS_INVALID("&c邮箱地址格式不正确。"),
    EMAIL_ALREADY_IN_USE("&c该邮箱已被其他账号绑定。"),
    EMAIL_ALREADY_BOUND("&c你的账号已经绑定了这个邮箱。"),
    EMAIL_BIND_SENT("&a验证码已发送至该邮箱, 使用 /bind code <验证码> 完成绑定。"),
    EMAIL_CHANGE_SENT("&a验证码已发送至原邮箱, 使用 /bind code <验证码> 验证原邮箱后继续更换。"),
    EMAIL_CHANGE_NEW_SENT("&a原邮箱验证成功, 验证码已发送至新邮箱, 使用 /bind code <验证码> 完成绑定。"),
    EMAIL_BIND_USAGE("&c使用 /bind email <邮箱> 获取验证码, 再使用 /bind code <验证码> 完成绑定。"),
    EMAIL_BIND_SUCCESS("&a邮箱绑定成功。"),
    EMAIL_BIND_FAILED("&c邮箱绑定失败, 请重新获取验证码。"),
    EMAIL_SENDING("&e正在发送验证码邮件, 请稍候……"),
    EMAIL_SEND_FAILED("&c验证码邮件发送失败, 请稍后重试。"),
    EMAIL_NO_BOUND_ADDRESS("&c该账号尚未绑定邮箱, 无法找回密码。"),
    EMAIL_RESET_SENT("&a密码重置验证码已发送, 使用 /fp <验证码> <新密码> <重复密码> 完成重置。"),
    EMAIL_RESET_USAGE("&c使用 /fp 获取验证码, 再使用 /fp <验证码> <新密码> <重复密码> 重置密码。"),
    EMAIL_CODE_MISSING("&c请先获取验证码。"),
    EMAIL_CODE_INVALID("&c验证码错误。"),
    EMAIL_CODE_EXPIRED("&c验证码已过期, 请重新获取。"),
    EMAIL_CODE_EXHAUSTED("&c验证码错误次数过多, 请重新获取。"),
    EMAIL_COOLDOWN("&c请等待 {0} 秒后再获取验证码。"),
    EMAIL_RESET_LOGIN_REQUIRED("&c当前无法找回密码, 请先确认账号处于等待登录状态。"),
    EMAIL_RESET_ALREADY_AUTHENTICATED("&e你已经登录, 无需找回密码。如需更换密码, 请使用 /cp <新密码> <重复新密码>。"),
    EMAIL_PASSWORD_RESET_SUCCESS("&a密码重置成功, 请使用新密码登录。"),
    EMAIL_PASSWORD_RESET_FAILED("&c密码重置失败, 请查看控制台。"),
    RESTRICT_CHAT_BLOCKED("&c你现在不能发言。"),
    RESTRICT_INTERACTION_BLOCKED("&c你现在不能这样做。"),
    ADMIN_ACCOUNT_MISSING("&c找不到玩家 {0} 的账号。"),
    ADMIN_PASSWORD_UPDATED("&c已重置玩家 {0} 的密码。"),
    ADMIN_PASSWORD_UPDATE_FAILED("&c修改密码失败, 请查看控制台。"),
    ADMIN_UNREGISTER_SUCCESS("&a已注销玩家 {0} 的账号。"),
    ADMIN_UNREGISTER_FAILED("&c注销账号失败, 请查看控制台。");

    private final String template;

    MessageKey(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }
}
