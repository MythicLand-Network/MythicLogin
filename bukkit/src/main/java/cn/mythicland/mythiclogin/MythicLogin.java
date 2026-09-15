package cn.mythicland.mythiclogin;

import cn.mythicland.framework.runtime.FrameworkRuntimeClassLoading;
import cn.mythicland.framework.runtime.RuntimeBootstrap;
import cn.mythicland.mythiclogin.bootstrap.RuntimeBootstrapFactory;
import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public final class MythicLogin extends JavaPlugin {
    private RuntimeBootstrap runtimeBootstrap;

    @Override
    public void onLoad() {
        initializeRuntime();
    }

    private void initializeRuntime() {
        if (this.runtimeBootstrap != null) return;
        saveDefaultConfig();
        String databaseMode = getConfig().getString("database.mode", "sqlite")
                .trim()
                .toLowerCase(Locale.ROOT);
        this.runtimeBootstrap = RuntimeBootstrapFactory.create(
                this,
                "cn.mythicland.mythiclogin.runtime.BukkitMainLoader",
                getDataFolder().toPath(),
                Set.of("common", "bukkit", "bukkit-email", "bukkit-" + databaseMode),
                getLogger()::info,
                getLogger()::warning,
                FrameworkRuntimeClassLoading.bukkit(
                        List.of(
                                "cn.mythicland.mythiclogin.",
                                "org.reflections.",
                                "javassist.",
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
    public void onEnable() {
        if (ProtocolLibrary.getProtocolManager() == null) {
            getLogger().severe("ProtocolLib is required for MythicLogin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            initializeRuntime();
            this.runtimeBootstrap.load();
            this.runtimeBootstrap.enable();
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Failed to initialize MythicLogin runtime.", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.runtimeBootstrap == null) return;
        this.runtimeBootstrap.disable();
        this.runtimeBootstrap = null;
    }

    public void reloadPlugin() {
        if (this.runtimeBootstrap == null) return;
        this.runtimeBootstrap.reload();
    }
}
