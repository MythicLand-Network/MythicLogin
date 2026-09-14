package cn.mythicland.mythiclogin.config.model;

import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;

public final class PluginSettings {

    private final DatabaseSettings database;
    private final SessionSettings session;
    private final RestrictionSettings restrictions;
    private final DisplaySettings display;

    public PluginSettings(DatabaseSettings database, SessionSettings session, RestrictionSettings restrictions, DisplaySettings display) {
        this.database = database;
        this.session = session;
        this.restrictions = restrictions;
        this.display = display;
    }

    public DatabaseSettings getDatabase() {
        return database;
    }

    public SessionSettings getSession() {
        return session;
    }

    public RestrictionSettings getRestrictions() {
        return restrictions;
    }

    public DisplaySettings getDisplay() {
        return display;
    }
}
