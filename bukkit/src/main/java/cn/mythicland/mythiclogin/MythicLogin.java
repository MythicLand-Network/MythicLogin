package cn.mythicland.mythiclogin;

import cn.mythicland.framework.bukkit.bootstrap.ManagedRuntimePlugin;
import cn.mythicland.framework.runtime.FrameworkRuntimeClassLoading;
import cn.mythicland.framework.runtime.RuntimeBootstrap;
import cn.mythicland.mythiclogin.bootstrap.RuntimeBootstrapFactory;
import com.comphenix.protocol.ProtocolLibrary;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public final class MythicLogin extends ManagedRuntimePlugin {
    @Override
    protected boolean prepareRuntimeOnLoad() {
        return true;
    }

    @Override
    protected RuntimeBootstrap createRuntime() {
        saveDefaultConfig();
        String databaseMode = getConfig().getString("database.mode", "sqlite")
                .trim()
                .toLowerCase(Locale.ROOT);
        return RuntimeBootstrapFactory.create(
                this,
                "cn.mythicland.mythiclogin.runtime.BukkitMainLoader",
                getDataFolder().toPath(),
                Set.of("common", "bukkit", "bukkit-email", "bukkit-" + databaseMode),
                getLogger()::info,
                getLogger()::warning,
                FrameworkRuntimeClassLoading.bukkit(
                        List.of(
                                "cn.mythicland.mythiclogin.",
                                "com.zaxxer.hikari.",
                                "org.flywaydb.",
                                "org.postgresql.",
                                "org.sqlite.",
                                "org.yaml.snakeyaml.",
                                "com.resend.",
                                "okhttp3.",
                                "okio.",
                                "kotlin.",
                                "org.jetbrains.",
                                "org.json.",
                                "com.fasterxml.jackson.",
                                "tools.jackson."
                        ),
                        List.of(MythicLogin.class.getName()),
                        List.of("cn.mythicland.mythiclogin.bootstrap.")
                )
        );
    }

    @Override
    protected boolean canEnableRuntime() {
        if (ProtocolLibrary.getProtocolManager() != null) return true;
        getLogger().severe("ProtocolLib is required for MythicLogin.");
        getServer().getPluginManager().disablePlugin(this);
        return false;
    }

    @Override
    protected void onEnableFailure(RuntimeException exception) {
        getLogger().log(Level.SEVERE, "Failed to initialize MythicLogin runtime.", exception);
    }

    public void reloadPlugin() {
        this.reloadRuntime();
    }
}
