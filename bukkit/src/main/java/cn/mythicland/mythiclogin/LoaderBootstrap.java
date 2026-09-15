package cn.mythicland.mythiclogin;

import cn.mythicland.framework.bukkit.BukkitComponentRegistrar;
import cn.mythicland.framework.command.CommandRouteCompiler;
import cn.mythicland.framework.component.ComponentContainer;
import cn.mythicland.framework.component.ComponentResolver;
import cn.mythicland.mythiclogin.email.EmailGateway;
import cn.mythicland.mythiclogin.email.ResendEmailGateway;
import cn.mythicland.mythiclogin.listener.StartupGateListener;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.service.AuthService;
import cn.mythicland.mythiclogin.service.EmailAuthService;
import cn.mythicland.mythiclogin.service.MessageService;
import cn.mythicland.mythiclogin.service.StartupGateService;
import cn.mythicland.mythiclogin.protocol.ProtocolGatekeeper;
import cn.mythicland.mythiclogin.service.VerificationCodeService;
import cn.mythicland.mythiclogin.util.PluginLogger;
import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.Bukkit;

import java.util.function.Consumer;

final class LoaderBootstrap {

    private final MythicLogin plugin;
    private final ComponentContainer components;
    private final PluginLogger pluginLogger;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final MessageService messageService;
    private final StartupGateService startupGateService;
    private final AuthService authService;
    private final EmailAuthService emailAuthService;
    private final ProtocolGatekeeper protocolGatekeeper;

    private LoaderBootstrap(
        MythicLogin plugin,
        ComponentContainer components,
        PluginLogger pluginLogger,
        ConfigManager configManager,
        DataManager dataManager,
        MessageService messageService,
        StartupGateService startupGateService,
        AuthService authService,
        EmailAuthService emailAuthService,
        ProtocolGatekeeper protocolGatekeeper
    ) {
        this.plugin = plugin;
        this.components = components;
        this.pluginLogger = pluginLogger;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.messageService = messageService;
        this.startupGateService = startupGateService;
        this.authService = authService;
        this.emailAuthService = emailAuthService;
        this.protocolGatekeeper = protocolGatekeeper;
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

        ComponentContainer components = new ComponentContainer();
        components.bind(plugin);
        components.bind(logger);
        components.bind(configManager);
        components.bind(dataManager);
        components.bind(ProtocolLibrary.getProtocolManager());
        components.bind(VerificationCodeService.class, new VerificationCodeService());
        components.bind(EmailGateway.class, new ResendEmailGateway(configManager));
        components.bind(Runnable.class, (Runnable) PluginLoader::reload);
        components.bind(Consumer.class, (Consumer<Runnable>) action -> runMainThread(plugin, action));
        components.bind(ComponentResolver.class, components);
        components.bind(new CommandRouteCompiler(components));
        components.scan(
                "cn.mythicland.mythiclogin",
                type -> !type.getName().startsWith("cn.mythicland.mythiclogin.velocity.")
        );

        MessageService messageService = components.resolve(MessageService.class);
        StartupGateService startupGateService = components.resolve(StartupGateService.class);
        AuthService authService = components.resolve(AuthService.class);
        EmailAuthService emailAuthService = components.resolve(EmailAuthService.class);
        emailAuthService.reload();
        return new LoaderBootstrap(
                plugin,
                components,
                logger,
                configManager,
                dataManager,
                messageService,
                startupGateService,
                authService,
                emailAuthService,
                components.resolve(ProtocolGatekeeper.class)
        );
    }

    void registerStartupGate() {
        startupGateService.markStarting();
        Bukkit.getPluginManager().registerEvents(components.resolve(StartupGateListener.class), plugin);
    }

    void registerListeners() {
        BukkitComponentRegistrar.register(plugin, components);
    }

    void finishStartup() {
        protocolGatekeeper.register();
        authService.start();
        startupGateService.markReady();
    }

    void reload() {
        configManager.reload();
        startupGateService.reload();
        dataManager.reload(configManager.getSettings().getDatabase());
        emailAuthService.reload();
        authService.reload();
    }

    void beginShutdown() {
        startupGateService.markStarting();
        protocolGatekeeper.unregister();
        emailAuthService.clear();
    }

    private static void runMainThread(MythicLogin plugin, Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, action);
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
