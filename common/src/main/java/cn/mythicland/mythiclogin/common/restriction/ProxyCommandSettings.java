package cn.mythicland.mythiclogin.common.restriction;

import java.util.List;

public final class ProxyCommandSettings {

    private final boolean enabled;
    private final CommandAccessMode mode;
    private final List<String> whitelist;
    private final List<String> blacklist;

    public ProxyCommandSettings(boolean enabled, CommandAccessMode mode, List<String> whitelist, List<String> blacklist) {
        this.enabled = enabled;
        this.mode = mode;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CommandAccessMode getMode() {
        return mode;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public List<String> getBlacklist() {
        return blacklist;
    }
}
