package cn.mythicland.mythiclogin.config.model;

public final class DisplaySettings {
    private static final DisplaySettings DEFAULTS = new DisplaySettings();

    private DisplaySettings() {
    }

    public static DisplaySettings defaults() {
        return DEFAULTS;
    }

    public int getRefreshIntervalTicks() {
        return 20;
    }

    public String getActionBarLoginText() {
        return "&7剩余时间: &f{0} &7秒";
    }

    public String getActionBarRegisterText() {
        return "&7剩余时间: &f{0} &7秒";
    }

    public int getCountdownSubtitleIntervalTicks() {
        return 20;
    }

    public int getCountdownSubtitleFadeIn() {
        return 0;
    }

    public int getCountdownSubtitleStay() {
        return 20;
    }

    public int getCountdownSubtitleFadeOut() {
        return 0;
    }

    public String getCountdownLoginTitle() {
        return "&c请登录!";
    }

    public String getCountdownLoginSubtitle() {
        return "&7使用 &f/login &f<密码> &7完成登录";
    }

    public String getCountdownRegisterTitle() {
        return "&c请注册!";
    }

    public String getCountdownRegisterSubtitle() {
        return "&7使用 &f/register &f<密码> <重复密码> &7完成注册";
    }

    public int getLoginFailureRestoreDelayTicks() {
        return 20;
    }

    public int getLoginFailureFadeIn() {
        return 0;
    }

    public int getLoginFailureStay() {
        return 20;
    }

    public int getLoginFailureFadeOut() {
        return 0;
    }

    public String getLoginFailureTitle() {
        return "&c密码错误!";
    }

    public String getLoginFailureSubtitle() {
        return "&7请重新输入正确密码";
    }

    public int getTitleFadeIn() {
        return 10;
    }

    public int getTitleStay() {
        return 20;
    }

    public int getTitleFadeOut() {
        return 10;
    }

    public String getLoginTitle() {
        return "&a登录成功";
    }

    public String getLoginSubtitle() {
        return "&7欢迎回来, &f{0}";
    }

    public String getRegisterTitle() {
        return "&a注册成功";
    }

    public String getRegisterSubtitle() {
        return "&7欢迎加入, &f{0}";
    }

    public String getAutoLoginTitle() {
        return "&a欢迎回来";
    }

    public String getAutoLoginSubtitle() {
        return "&7已为您自动登录。";
    }
}
