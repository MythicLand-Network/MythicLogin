package cn.mythicland.mythiclogin;

import cn.mythicland.mythiclogin.bootstrap.RuntimeBootstrap;
import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Set;

public final class MythicLogin extends JavaPlugin {
    private RuntimeBootstrap runtimeBootstrap;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        String databaseMode = getConfig().getString("database.mode", "sqlite")
                .trim()
                .toLowerCase(Locale.ROOT);
        this.runtimeBootstrap = new RuntimeBootstrap(
                this,
                "cn.mythicland.mythiclogin.runtime.BukkitMainLoader",
                MythicLogin.class,
                getDataFolder().toPath(),
                Set.of("common", "bukkit", "bukkit-" + databaseMode),
                getLogger()::info
        );
        this.runtimeBootstrap.load();
    }

    @Override
    public void onEnable() {
        if (ProtocolLibrary.getProtocolManager() == null) {
            getLogger().severe("ProtocolLib is required for MythicLogin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.runtimeBootstrap.enable();
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
