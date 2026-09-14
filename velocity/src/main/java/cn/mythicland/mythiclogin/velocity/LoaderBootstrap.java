package cn.mythicland.mythiclogin.velocity;

import cn.mythicland.mythiclogin.velocity.config.ProxyConfig;
import cn.mythicland.mythiclogin.velocity.listener.ProxyCommandListener;
import cn.mythicland.mythiclogin.velocity.listener.ProxyPreLoginListener;
import cn.mythicland.mythiclogin.velocity.listener.ProxySessionListener;
import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import cn.mythicland.mythiclogin.velocity.manager.ProxyDataManager;
import cn.mythicland.mythiclogin.velocity.manager.ProxyRestrictionManagerImpl;
import cn.mythicland.mythiclogin.velocity.service.MessageService;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;

final class LoaderBootstrap {

    private final PluginLogger pluginLogger;
    private final ProxyConfig proxyConfig;
    private final ProxyDataManager dataManager;
    private final MessageService messageService;
    private final IProxyRestrictionManager proxyRestrictionManager;

    private LoaderBootstrap(
        PluginLogger pluginLogger,
        ProxyConfig proxyConfig,
        ProxyDataManager dataManager,
        MessageService messageService,
        IProxyRestrictionManager proxyRestrictionManager
    ) {
        this.pluginLogger = pluginLogger;
        this.proxyConfig = proxyConfig;
        this.dataManager = dataManager;
        this.messageService = messageService;
        this.proxyRestrictionManager = proxyRestrictionManager;
    }

    static LoaderBootstrap create(MythicLoginVelocity plugin) {
        ProxyConfig proxyConfig = new ProxyConfig(plugin.getDataDirectory(), plugin.getClass().getClassLoader());
        PluginLogger logger = new PluginLogger(plugin.getLogger(), "MythicLoginVelocity");

        ProxyDataManager dataManager;
        try {
            dataManager = new ProxyDataManager(logger, proxyConfig.getSettings().getDatabase());
        } catch (IllegalStateException exception) {
            logger.severe("Failed to initialize MythicLoginVelocity database.", exception);
            throw exception;
        }

        MessageService messageService = new MessageService();
        IProxyRestrictionManager proxyRestrictionManager = new ProxyRestrictionManagerImpl(
            proxyConfig.getSettings(),
            messageService,
            dataManager.getAccountRepository(),
            logger
        );
        return new LoaderBootstrap(logger, proxyConfig, dataManager, messageService, proxyRestrictionManager);
    }

    void registerListeners(MythicLoginVelocity plugin) {
        plugin.getProxyServer().getEventManager().register(plugin, new ProxyPreLoginListener(proxyRestrictionManager));
        plugin.getProxyServer().getEventManager().register(plugin, new ProxyCommandListener(proxyRestrictionManager));
        plugin.getProxyServer().getEventManager().register(plugin, new ProxySessionListener(proxyRestrictionManager));
    }

    void finishStartup() {
        proxyRestrictionManager.runStartupSanityCheck();
    }

    PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    ProxyDataManager getDataManager() {
        return dataManager;
    }

    MessageService getMessageService() {
        return messageService;
    }

    IProxyRestrictionManager getProxyRestrictionManager() {
        return proxyRestrictionManager;
    }
}
