package cn.mythicland.mythiclogin.velocity.config;

import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.common.restriction.ProxyCommandSettings;

public final class PluginSettings {

    private final DatabaseSettings database;
    private final SessionSettings session;
    private final LoginServerSettings loginServers;
    private final ProxyCommandSettings proxyCommands;

    public PluginSettings(
        DatabaseSettings database,
        SessionSettings session,
        LoginServerSettings loginServers,
        ProxyCommandSettings proxyCommands
    ) {
        this.database = database;
        this.session = session;
        this.loginServers = loginServers;
        this.proxyCommands = proxyCommands;
    }

    public DatabaseSettings getDatabase() {
        return database;
    }

    public SessionSettings getSession() {
        return session;
    }

    public LoginServerSettings getLoginServers() {
        return loginServers;
    }

    public ProxyCommandSettings getProxyCommands() {
        return proxyCommands;
    }
}
