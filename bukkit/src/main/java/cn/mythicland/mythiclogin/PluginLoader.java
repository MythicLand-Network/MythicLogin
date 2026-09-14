package cn.mythicland.mythiclogin;

import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.service.AuthService;
import cn.mythicland.mythiclogin.service.MessageService;
import cn.mythicland.mythiclogin.service.StartupGateService;
import cn.mythicland.mythiclogin.util.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

public final class PluginLoader {

    private static MythicLogin instance;
    private static PluginLogger pluginLogger;
    private static ConfigManager configManager;
    private static DataManager dataManager;
    private static AuthService authService;
    private static MessageService messageService;
    private static StartupGateService startupGateService;
    private static LoaderBootstrap bootstrap;

    private PluginLoader() {
    }

    public static void onLoad(MythicLogin plugin) {
        instance = plugin;
        bootstrap = LoaderBootstrap.create(plugin);
        applyBootstrap(bootstrap);
    }

    public static void onEnable() {
        if (bootstrap == null && instance != null) {
            bootstrap = LoaderBootstrap.create(instance);
            applyBootstrap(bootstrap);
        }
        if (bootstrap == null) {
            return;
        }
        try {
            bootstrap.registerStartupGate();
            bootstrap.registerCommands();
            bootstrap.registerListeners();
            bootstrap.finishStartup();
        } catch (IllegalStateException exception) {
            if (pluginLogger != null) {
                pluginLogger.severe("Failed to enable plugin.", exception);
            }
            Bukkit.getPluginManager().disablePlugin(instance);
        }
    }

    public static void onDisable() {
        if (bootstrap != null) {
            bootstrap.beginShutdown();
        }
        if (instance != null) {
            HandlerList.unregisterAll(instance);
        }
        if (authService != null) {
            authService.shutdown();
        }
        if (dataManager != null) {
            dataManager.shutdown();
        }
        bootstrap = null;
        authService = null;
        messageService = null;
        startupGateService = null;
        dataManager = null;
        configManager = null;
        pluginLogger = null;
        instance = null;
    }

    public static void reload() {
        if (bootstrap != null) {
            bootstrap.reload();
        }
    }

    public static MythicLogin getInstance() {
        return instance;
    }

    public static PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static DataManager getDataManager() {
        return dataManager;
    }

    public static AuthService getAuthService() {
        return authService;
    }

    public static MessageService getMessageService() {
        return messageService;
    }

    private static void applyBootstrap(LoaderBootstrap loaderBootstrap) {
        pluginLogger = loaderBootstrap.getPluginLogger();
        configManager = loaderBootstrap.getConfigManager();
        dataManager = loaderBootstrap.getDataManager();
        authService = loaderBootstrap.getAuthService();
        messageService = loaderBootstrap.getMessageService();
        startupGateService = loaderBootstrap.getStartupGateService();
    }
}
