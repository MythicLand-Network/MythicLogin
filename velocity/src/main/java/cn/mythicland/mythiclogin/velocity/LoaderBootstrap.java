package cn.mythicland.mythiclogin.velocity;

import cn.mythicland.framework.component.ComponentContainer;
import cn.mythicland.mythiclogin.velocity.config.ProxyConfig;
import cn.mythicland.mythiclogin.velocity.listener.ProxyCommandListener;
import cn.mythicland.mythiclogin.velocity.listener.ProxyPreLoginListener;
import cn.mythicland.mythiclogin.velocity.listener.ProxySessionListener;
import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import cn.mythicland.mythiclogin.velocity.manager.ProxyDataManager;
import cn.mythicland.mythiclogin.velocity.service.MessageService;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;

final class LoaderBootstrap {

    private final ComponentContainer components;
    private final PluginLogger pluginLogger;
    private final ProxyConfig proxyConfig;
    private final ProxyDataManager dataManager;
    private final MessageService messageService;
    private final IProxyRestrictionManager proxyRestrictionManager;

    private LoaderBootstrap(
        ComponentContainer components,
        PluginLogger pluginLogger,
        ProxyConfig proxyConfig,
        ProxyDataManager dataManager,
        MessageService messageService,
        IProxyRestrictionManager proxyRestrictionManager
    ) {
        this.components = components;
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

        ComponentContainer components = new ComponentContainer();
        components.bind(proxyConfig.getSettings());
        components.bind(dataManager.getAccountRepository());
        components.bind(logger);
        components.scan("cn.mythicland.mythiclogin.velocity");

        return new LoaderBootstrap(
                components,
                logger,
                proxyConfig,
                dataManager,
                components.resolve(MessageService.class),
                components.resolve(IProxyRestrictionManager.class)
        );
    }

    void registerListeners(MythicLoginVelocity plugin) {
        plugin.getProxyServer().getEventManager().register(plugin, components.resolve(ProxyPreLoginListener.class));
        plugin.getProxyServer().getEventManager().register(plugin, components.resolve(ProxyCommandListener.class));
        plugin.getProxyServer().getEventManager().register(plugin, components.resolve(ProxySessionListener.class));
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
