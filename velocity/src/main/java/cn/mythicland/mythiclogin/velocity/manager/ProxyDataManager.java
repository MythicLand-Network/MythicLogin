package cn.mythicland.mythiclogin.velocity.manager;

import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseManager;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.infrastructure.database.JdbcAccountRepository;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;

import java.util.logging.Logger;

public final class ProxyDataManager {

    private final PluginLogger pluginLogger;
    private DatabaseManager databaseManager;
    private AccountRepository accountRepository;

    public ProxyDataManager(PluginLogger pluginLogger, DatabaseSettings settings) {
        this.pluginLogger = pluginLogger;
        initialize(settings);
    }

    private void initialize(DatabaseSettings settings) {
        databaseManager = new DatabaseManager(Logger.getLogger("MythicLoginVelocity"), settings, "MythicLoginVelocity");
        accountRepository = new JdbcAccountRepository(databaseManager, () -> settings);
    }

    public void reload(DatabaseSettings settings) {
        databaseManager.reloadIfNecessary(settings);
        accountRepository = new JdbcAccountRepository(databaseManager, () -> settings);
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    public void shutdown() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
}
