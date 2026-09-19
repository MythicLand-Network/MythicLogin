package cn.mythicland.mythiclogin.config.model;

import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;

public record PluginSettings(
        DatabaseSettings database,
        SessionSettings session,
        RestrictionSettings restrictions,
        DisplaySettings display
) {
}
