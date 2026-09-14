package cn.mythicland.mythiclogin.velocity.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class LoginServerSettings {

    private final boolean allServersLogin;
    private final List<String> names;

    public LoginServerSettings(boolean allServersLogin, List<String> names) {
        this.allServersLogin = allServersLogin;
        this.names = normalizeNames(names);
    }

    public boolean isAllServersLogin() {
        return allServersLogin;
    }

    public List<String> getNames() {
        return names;
    }

    public boolean contains(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return false;
        }
        return names.contains(serverName.trim().toLowerCase(Locale.ROOT));
    }

    private List<String> normalizeNames(List<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        for (String rawName : rawNames) {
            if (rawName == null) {
                continue;
            }
            String value = rawName.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty() || normalized.contains(value)) {
                continue;
            }
            normalized.add(value);
        }
        return Collections.unmodifiableList(normalized);
    }
}
