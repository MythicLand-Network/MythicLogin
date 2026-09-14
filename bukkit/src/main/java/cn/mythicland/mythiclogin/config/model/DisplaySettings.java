package cn.mythicland.mythiclogin.config.model;

import org.bukkit.configuration.file.YamlConfiguration;

public final class DisplaySettings {

    private final int refreshIntervalTicks;
    private final String actionBarLoginText;
    private final String actionBarRegisterText;
    private final int countdownSubtitleIntervalTicks;
    private final int countdownSubtitleFadeIn;
    private final int countdownSubtitleStay;
    private final int countdownSubtitleFadeOut;
    private final String countdownLoginTitle;
    private final String countdownLoginSubtitle;
    private final String countdownRegisterTitle;
    private final String countdownRegisterSubtitle;
    private final int loginFailureRestoreDelayTicks;
    private final int loginFailureFadeIn;
    private final int loginFailureStay;
    private final int loginFailureFadeOut;
    private final String loginFailureTitle;
    private final String loginFailureSubtitle;
    private final int titleFadeIn;
    private final int titleStay;
    private final int titleFadeOut;
    private final String loginTitle;
    private final String loginSubtitle;
    private final String registerTitle;
    private final String registerSubtitle;
    private final String autoLoginTitle;
    private final String autoLoginSubtitle;

    public DisplaySettings(int refreshIntervalTicks, String actionBarLoginText, String actionBarRegisterText, int countdownSubtitleIntervalTicks, int countdownSubtitleFadeIn, int countdownSubtitleStay, int countdownSubtitleFadeOut, String countdownLoginTitle, String countdownLoginSubtitle, String countdownRegisterTitle, String countdownRegisterSubtitle, int loginFailureRestoreDelayTicks, int loginFailureFadeIn, int loginFailureStay, int loginFailureFadeOut, String loginFailureTitle, String loginFailureSubtitle, int titleFadeIn, int titleStay, int titleFadeOut, String loginTitle, String loginSubtitle, String registerTitle, String registerSubtitle, String autoLoginTitle, String autoLoginSubtitle) {
        this.refreshIntervalTicks = refreshIntervalTicks;
        this.actionBarLoginText = actionBarLoginText;
        this.actionBarRegisterText = actionBarRegisterText;
        this.countdownSubtitleIntervalTicks = countdownSubtitleIntervalTicks;
        this.countdownSubtitleFadeIn = countdownSubtitleFadeIn;
        this.countdownSubtitleStay = countdownSubtitleStay;
        this.countdownSubtitleFadeOut = countdownSubtitleFadeOut;
        this.countdownLoginTitle = countdownLoginTitle;
        this.countdownLoginSubtitle = countdownLoginSubtitle;
        this.countdownRegisterTitle = countdownRegisterTitle;
        this.countdownRegisterSubtitle = countdownRegisterSubtitle;
        this.loginFailureRestoreDelayTicks = loginFailureRestoreDelayTicks;
        this.loginFailureFadeIn = loginFailureFadeIn;
        this.loginFailureStay = loginFailureStay;
        this.loginFailureFadeOut = loginFailureFadeOut;
        this.loginFailureTitle = loginFailureTitle;
        this.loginFailureSubtitle = loginFailureSubtitle;
        this.titleFadeIn = titleFadeIn;
        this.titleStay = titleStay;
        this.titleFadeOut = titleFadeOut;
        this.loginTitle = loginTitle;
        this.loginSubtitle = loginSubtitle;
        this.registerTitle = registerTitle;
        this.registerSubtitle = registerSubtitle;
        this.autoLoginTitle = autoLoginTitle;
        this.autoLoginSubtitle = autoLoginSubtitle;
    }

    public static DisplaySettings fromConfig(YamlConfiguration config) {
        return new DisplaySettings(
                20,
                config.getString("display.action-bar.login-text", "&7剩余时间: &f{0} &7秒"),
                config.getString("display.action-bar.register-text", "&7剩余时间: &f{0} &7秒"),
                config.getInt("display.text.subtitle.subtitle-interval-ticks", 20),
                config.getInt("display.text.subtitle.subtitle-fade-in", 0),
                config.getInt("display.text.subtitle.subtitle-stay", 20),
                config.getInt("display.text.subtitle.subtitle-fade-out", 0),
                config.getString("display.text.title.login-title", "&c请登录!"),
                config.getString("display.text.subtitle.login-subtitle", "&7使用 &f/login &f<密码> &7完成登录"),
                config.getString("display.text.title.register-title", "&c请注册!"),
                config.getString("display.text.subtitle.register-subtitle", "&7使用 &f/register &f<密码> <重复密码> &7完成注册"),
                config.getInt("display.text.failure-title.restore-delay-ticks", 20),
                config.getInt("display.text.failure-title.fade-in", 0),
                config.getInt("display.text.failure-title.stay", 20),
                config.getInt("display.text.failure-title.fade-out", 0),
                config.getString("display.text.failure-title.login-title", "&c密码错误!"),
                config.getString("display.text.failure-title.login-subtitle", "&7请重新输入正确密码"),
                config.getInt("display.text.success-title.fade-in", 10),
                config.getInt("display.text.success-title.stay", 20),
                config.getInt("display.text.success-title.fade-out", 10),
                config.getString("display.text.success-title.login-title", "&a登录成功"),
                config.getString("display.text.success-title.login-subtitle", "&7欢迎回来, &f{0}"),
                config.getString("display.text.success-title.register-title", "&a注册成功"),
                config.getString("display.text.success-title.register-subtitle", "&7欢迎加入, &f{0}"),
                config.getString("display.text.success-title.auto-login-title", "&a欢迎回来"),
                config.getString("display.text.success-title.auto-login-subtitle", "&7已为您自动登录。")
        );
    }

    public int getRefreshIntervalTicks() {
        return refreshIntervalTicks;
    }

    public String getActionBarLoginText() {
        return actionBarLoginText;
    }

    public String getActionBarRegisterText() {
        return actionBarRegisterText;
    }

    public int getCountdownSubtitleIntervalTicks() {
        return countdownSubtitleIntervalTicks;
    }

    public int getCountdownSubtitleFadeIn() {
        return countdownSubtitleFadeIn;
    }

    public int getCountdownSubtitleStay() {
        return countdownSubtitleStay;
    }

    public int getCountdownSubtitleFadeOut() {
        return countdownSubtitleFadeOut;
    }

    public String getCountdownLoginTitle() {
        return countdownLoginTitle;
    }

    public String getCountdownLoginSubtitle() {
        return countdownLoginSubtitle;
    }

    public String getCountdownRegisterTitle() {
        return countdownRegisterTitle;
    }

    public String getCountdownRegisterSubtitle() {
        return countdownRegisterSubtitle;
    }

    public int getLoginFailureRestoreDelayTicks() {
        return loginFailureRestoreDelayTicks;
    }

    public int getLoginFailureFadeIn() {
        return loginFailureFadeIn;
    }

    public int getLoginFailureStay() {
        return loginFailureStay;
    }

    public int getLoginFailureFadeOut() {
        return loginFailureFadeOut;
    }

    public String getLoginFailureTitle() {
        return loginFailureTitle;
    }

    public String getLoginFailureSubtitle() {
        return loginFailureSubtitle;
    }

    public int getTitleFadeIn() {
        return titleFadeIn;
    }

    public int getTitleStay() {
        return titleStay;
    }

    public int getTitleFadeOut() {
        return titleFadeOut;
    }

    public String getLoginTitle() {
        return loginTitle;
    }

    public String getLoginSubtitle() {
        return loginSubtitle;
    }

    public String getRegisterTitle() {
        return registerTitle;
    }

    public String getRegisterSubtitle() {
        return registerSubtitle;
    }

    public String getAutoLoginTitle() {
        return autoLoginTitle;
    }

    public String getAutoLoginSubtitle() {
        return autoLoginSubtitle;
    }
}
