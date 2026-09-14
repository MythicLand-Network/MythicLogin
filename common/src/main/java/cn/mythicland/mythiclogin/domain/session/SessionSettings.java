package cn.mythicland.mythiclogin.domain.session;

public record SessionSettings(boolean enabled, int timeoutMinutes) {
    public boolean isEnabled() {
        return this.enabled;
    }

    public int getTimeoutMinutes() {
        return this.timeoutMinutes;
    }
}
