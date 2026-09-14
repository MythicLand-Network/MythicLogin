package cn.mythicland.mythiclogin.config.model;

import java.util.List;
import java.util.Locale;

public final class RestrictionSettings {

    private final List<String> commandPrefixWhitelist;
    private final boolean blockNonWhitelistTabComplete;
    private final boolean blockChat;
    private final boolean kickOnTimeout;
    private final int authTimeoutSeconds;
    private final int minPasswordLength;
    private final int maxPasswordLength;
    private final boolean blockBungeeServerCommand;

    public RestrictionSettings(List<String> commandPrefixWhitelist, boolean blockNonWhitelistTabComplete, boolean blockChat, boolean kickOnTimeout, int authTimeoutSeconds, int minPasswordLength, int maxPasswordLength, boolean blockBungeeServerCommand) {
        this.commandPrefixWhitelist = commandPrefixWhitelist;
        this.blockNonWhitelistTabComplete = blockNonWhitelistTabComplete;
        this.blockChat = blockChat;
        this.kickOnTimeout = kickOnTimeout;
        this.authTimeoutSeconds = authTimeoutSeconds;
        this.minPasswordLength = minPasswordLength;
        this.maxPasswordLength = maxPasswordLength;
        this.blockBungeeServerCommand = blockBungeeServerCommand;
    }

    public List<String> getCommandPrefixWhitelist() {
        return commandPrefixWhitelist;
    }

    public boolean isBlockNonWhitelistTabComplete() {
        return blockNonWhitelistTabComplete;
    }

    public boolean isBlockChat() {
        return blockChat;
    }

    public boolean isKickOnTimeout() {
        return kickOnTimeout;
    }

    public int getAuthTimeoutSeconds() {
        return authTimeoutSeconds;
    }

    public int getMinPasswordLength() {
        return minPasswordLength;
    }

    public int getMaxPasswordLength() {
        return maxPasswordLength;
    }

    public boolean isCommandAllowed(String rawCommand) {
        String normalized = normalizeCommandInput(rawCommand);
        if (blockBungeeServerCommand && (normalized.equals("/server") || normalized.startsWith("/server "))) {
            return false;
        }
        for (String prefix : commandPrefixWhitelist) {
            if (normalized.equals(prefix) || normalized.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    public boolean isTabCompleteAllowed(String rawCommand) {
        String normalized = normalizeCommandInput(rawCommand);
        if ("/".equals(normalized)) {
            return true;
        }
        if (blockBungeeServerCommand && "/server".startsWith(normalized)) {
            return false;
        }
        for (String prefix : commandPrefixWhitelist) {
            if (prefix.startsWith(normalized)) {
                return true;
            }
            if (normalized.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    public String normalizeCommandInput(String command) {
        String normalized = command == null ? "" : command.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }
}
