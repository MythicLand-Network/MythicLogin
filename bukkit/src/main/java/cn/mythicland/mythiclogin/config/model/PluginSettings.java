package cn.mythicland.mythiclogin.config.model;

import cn.mythicland.mythiclogin.common.premium.PremiumSettings;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;

public record PluginSettings(
        DatabaseSettings database,
        SessionSettings session,
        RestrictionSettings restrictions,
        DisplaySettings display,
        PremiumSettings premium
) {
    public PluginSettings(
            DatabaseSettings database,
            SessionSettings session,
            RestrictionSettings restrictions,
            DisplaySettings display
    ) {
        this(database, session, restrictions, display, PremiumSettings.disabled());
    }
}
