package cn.mythicland.mythiclogin.common.restriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class CommandListMatcher {

    private final CommandAccessMode mode;
    private final List<String> whitelist;
    private final List<String> blacklist;

    public CommandListMatcher(ProxyCommandSettings settings) {
        this(settings.getMode(), settings.getWhitelist(), settings.getBlacklist());
    }

    public CommandListMatcher(CommandAccessMode mode, List<String> whitelist, List<String> blacklist) {
        this.mode = mode;
        this.whitelist = normalizeEntries(whitelist);
        this.blacklist = normalizeEntries(blacklist);
    }

    public boolean shouldBlock(String rawCommand) {
        String normalized = normalizeCommandInput(rawCommand);
        if (normalized.length() <= 1) {
            return false;
        }
        boolean listed = matches(mode == CommandAccessMode.WHITELIST ? whitelist : blacklist, normalized);
        if (mode == CommandAccessMode.WHITELIST) {
            return !listed;
        }
        return listed;
    }

    public boolean isAllowed(String rawCommand) {
        return !shouldBlock(rawCommand);
    }

    public List<String> filterSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (!shouldBlock(suggestion)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

    public String normalizeCommandInput(String command) {
        String normalized = command == null ? "" : command.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private boolean matches(List<String> entries, String command) {
        for (String entry : entries) {
            if (command.equals(entry) || command.startsWith(entry + " ")) {
                return true;
            }
        }
        return false;
    }

    private List<String> normalizeEntries(List<String> commands) {
        List<String> normalized = new ArrayList<>();
        if (commands == null) {
            return normalized;
        }
        for (String command : commands) {
            String value = normalizeCommandInput(command);
            if (value.length() <= 1) {
                continue;
            }
            normalized.add(value);
        }
        return normalized;
    }
}
