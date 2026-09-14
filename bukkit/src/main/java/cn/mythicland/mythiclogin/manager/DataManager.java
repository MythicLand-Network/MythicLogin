package cn.mythicland.mythiclogin.manager;

import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseManager;
import cn.mythicland.mythiclogin.infrastructure.database.JdbcAccountRepository;
import cn.mythicland.mythiclogin.util.PluginLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DataManager {

    private final PluginLogger pluginLogger;
    private final ExecutorService executorService;
    private DatabaseManager databaseManager;
    private AccountRepository accountRepository;

    public DataManager(PluginLogger pluginLogger, DatabaseSettings settings) {
        this.pluginLogger = pluginLogger;
        this.executorService = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "MythicLogin-Worker");
            thread.setDaemon(true);
            return thread;
        });
        initialize(settings);
    }

    private void initialize(DatabaseSettings settings) {
        databaseManager = new DatabaseManager(pluginLogger.getDelegate(), settings, "MythicLogin");
        accountRepository = new JdbcAccountRepository(databaseManager, () -> settings);
    }

    public void reload(DatabaseSettings settings) {
        databaseManager.reloadIfNecessary(settings);
        accountRepository = new JdbcAccountRepository(databaseManager, () -> settings);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public AccountRepository getAccountRepository() {
        return accountRepository;
    }

    public void shutdown() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        executorService.shutdownNow();
    }
}
