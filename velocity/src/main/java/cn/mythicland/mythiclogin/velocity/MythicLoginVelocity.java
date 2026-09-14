package cn.mythicland.mythiclogin.velocity;

import cn.mythicland.mythiclogin.bootstrap.RuntimeBootstrap;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Set;

@Plugin(
    id = "mythicloginvelocity",
    name = "MythicLoginVelocity",
    version = "1.0.0",
    authors = {"bilib"}
)
public final class MythicLoginVelocity {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private RuntimeBootstrap runtimeBootstrap;

    @Inject
    public MythicLoginVelocity(
        ProxyServer proxyServer,
        Logger logger,
        @DataDirectory Path dataDirectory
    ) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.runtimeBootstrap = new RuntimeBootstrap(
                this,
                "cn.mythicland.mythiclogin.velocity.runtime.VelocityMainLoader",
                MythicLoginVelocity.class,
                this.dataDirectory,
                Set.of("common", "velocity", "velocity-postgresql"),
                this.logger::info
        );
        this.runtimeBootstrap.load();
        this.runtimeBootstrap.enable();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (this.runtimeBootstrap == null) return;
        this.runtimeBootstrap.disable();
        this.runtimeBootstrap = null;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}
