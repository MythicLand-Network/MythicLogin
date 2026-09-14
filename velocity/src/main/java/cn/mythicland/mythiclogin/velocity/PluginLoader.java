package cn.mythicland.mythiclogin.velocity;

import cn.mythicland.mythiclogin.velocity.config.ProxyConfig;
import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import cn.mythicland.mythiclogin.velocity.manager.ProxyDataManager;
import cn.mythicland.mythiclogin.velocity.service.MessageService;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;

public final class PluginLoader {

    private static MythicLoginVelocity instance;
    private static PluginLogger pluginLogger;
    private static ProxyConfig proxyConfig;
    private static ProxyDataManager dataManager;
    private static MessageService messageService;
    private static IProxyRestrictionManager proxyRestrictionManager;
    private static LoaderBootstrap bootstrap;

    private PluginLoader() {
    }

    public static void onEnable(MythicLoginVelocity plugin) {
        instance = plugin;
        bootstrap = LoaderBootstrap.create(plugin);
        applyBootstrap(bootstrap);
        bootstrap.registerListeners(plugin);
        bootstrap.finishStartup();
    }

    public static void onDisable() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
        bootstrap = null;
        proxyRestrictionManager = null;
        messageService = null;
        dataManager = null;
        proxyConfig = null;
        pluginLogger = null;
        instance = null;
    }

    public static MythicLoginVelocity getInstance() {
        return instance;
    }

    public static PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    public static ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public static ProxyDataManager getDataManager() {
        return dataManager;
    }

    public static MessageService getMessageService() {
        return messageService;
    }

    public static IProxyRestrictionManager getProxyRestrictionManager() {
        return proxyRestrictionManager;
    }

    private static void applyBootstrap(LoaderBootstrap loaderBootstrap) {
        pluginLogger = loaderBootstrap.getPluginLogger();
        proxyConfig = loaderBootstrap.getProxyConfig();
        dataManager = loaderBootstrap.getDataManager();
        messageService = loaderBootstrap.getMessageService();
        proxyRestrictionManager = loaderBootstrap.getProxyRestrictionManager();
    }
}
