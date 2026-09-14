package cn.mythicland.mythiclogin;

import cn.mythicland.mythiclogin.command.AdminCommands;
import cn.mythicland.mythiclogin.command.AuthCommands;
import cn.mythicland.mythiclogin.listener.CaseSensitiveAccountListener;
import cn.mythicland.mythiclogin.listener.FallbackRestrictionListener;
import cn.mythicland.mythiclogin.listener.PlayerConnectionListener;
import cn.mythicland.mythiclogin.listener.SensitiveAuthCommandListener;
import cn.mythicland.mythiclogin.listener.StartupGateListener;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.service.AuthService;
import cn.mythicland.mythiclogin.service.AuthSessionManager;
import cn.mythicland.mythiclogin.service.DisplayService;
import cn.mythicland.mythiclogin.service.MessageService;
import cn.mythicland.mythiclogin.service.PasswordService;
import cn.mythicland.mythiclogin.service.PlayerCommandService;
import cn.mythicland.mythiclogin.service.RestrictionService;
import cn.mythicland.mythiclogin.service.StartupGateService;
import cn.mythicland.mythiclogin.protocol.ProtocolGatekeeper;
import cn.mythicland.mythiclogin.util.PluginLogger;
import cn.mythicland.framework.command.BukkitCommandRegistrar;
import cn.mythicland.framework.command.CommandRouteCompiler;
import cn.mythicland.framework.component.ComponentContainer;
import cn.mythicland.framework.component.ComponentResolver;
import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicReference;

final class LoaderBootstrap {

    private final MythicLogin plugin;
    private final PluginLogger pluginLogger;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final MessageService messageService;
    private final StartupGateService startupGateService;
    private final AuthService authService;
    private final PlayerCommandService playerCommandService;

    private LoaderBootstrap(
        MythicLogin plugin,
        PluginLogger pluginLogger,
        ConfigManager configManager,
        DataManager dataManager,
        MessageService messageService,
        StartupGateService startupGateService,
        AuthService authService,
        PlayerCommandService playerCommandService
    ) {
        this.plugin = plugin;
        this.pluginLogger = pluginLogger;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.messageService = messageService;
        this.startupGateService = startupGateService;
        this.authService = authService;
        this.playerCommandService = playerCommandService;
    }

    static LoaderBootstrap create(MythicLogin plugin) {
        PluginLogger logger = new PluginLogger(plugin.getLogger(), "MythicLogin");
        ConfigManager configManager = new ConfigManager(plugin);

        DataManager dataManager;
        try {
            dataManager = new DataManager(logger, configManager.getSettings().getDatabase());
        } catch (IllegalStateException exception) {
            logger.severe("Failed to initialize MythicLogin database.", exception);
            throw exception;
        }

        MessageService messageService = new MessageService(configManager);
        StartupGateService startupGateService = new StartupGateService(messageService);
        AuthSessionManager authSessionManager = new AuthSessionManager();
        DisplayService displayService = new DisplayService(plugin, configManager, messageService, logger);
        RestrictionService restrictionService = new RestrictionService(plugin, configManager, authSessionManager);
        PasswordService passwordService = new PasswordService();
        AtomicReference<PlayerCommandService> playerCommandServiceReference = new AtomicReference<>();
        ProtocolGatekeeper protocolGatekeeper = new ProtocolGatekeeper(
            plugin,
            ProtocolLibrary.getProtocolManager(),
            authSessionManager,
            configManager,
            messageService,
            playerCommandServiceReference::get,
            logger
        );
        AuthService authService = new AuthService(
            plugin,
            logger,
            configManager,
            dataManager,
            messageService,
            passwordService,
            displayService,
            authSessionManager,
            restrictionService,
            protocolGatekeeper
        );
        PlayerCommandService playerCommandService = new PlayerCommandService(authService, messageService, PluginLoader::reload);
        playerCommandServiceReference.set(playerCommandService);
        return new LoaderBootstrap(plugin, logger, configManager, dataManager, messageService, startupGateService, authService, playerCommandService);
    }

    void registerStartupGate() {
        startupGateService.markStarting();
        Bukkit.getPluginManager().registerEvents(new StartupGateListener(startupGateService), plugin);
    }

    void registerCommands() {
        ComponentContainer components = new ComponentContainer();
        components.bind(ComponentResolver.class, components);
        components.bind(new CommandRouteCompiler(components));
        components.bind(new AuthCommands(this.playerCommandService));
        components.bind(new AdminCommands(this.playerCommandService));
        BukkitCommandRegistrar.register(this.plugin, components);
    }

    void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CaseSensitiveAccountListener(authService), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(authService), plugin);
        Bukkit.getPluginManager().registerEvents(new SensitiveAuthCommandListener(playerCommandService), plugin);
        Bukkit.getPluginManager().registerEvents(new FallbackRestrictionListener(authService, messageService), plugin);
    }

    void finishStartup() {
        authService.start();
        startupGateService.markReady();
    }

    void reload() {
        configManager.reload();
        messageService.reload();
        startupGateService.reload();
        dataManager.reload(configManager.getSettings().getDatabase());
        authService.reload();
    }

    void beginShutdown() {
        startupGateService.markStarting();
    }

    PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    ConfigManager getConfigManager() {
        return configManager;
    }

    DataManager getDataManager() {
        return dataManager;
    }

    MessageService getMessageService() {
        return messageService;
    }

    StartupGateService getStartupGateService() {
        return startupGateService;
    }

    AuthService getAuthService() {
        return authService;
    }
}
