package cn.mythicland.mythiclogin.runtime;

import cn.mythicland.mythiclogin.MythicLogin;
import cn.mythicland.mythiclogin.PluginLoader;

public final class BukkitMainLoader {
    private final MythicLogin plugin;

    public BukkitMainLoader(MythicLogin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        PluginLoader.onLoad(this.plugin);
    }

    public void enable() {
        PluginLoader.onEnable();
    }

    public void reload() {
        PluginLoader.reload();
    }

    public void unload() {
        PluginLoader.onDisable();
    }
}
