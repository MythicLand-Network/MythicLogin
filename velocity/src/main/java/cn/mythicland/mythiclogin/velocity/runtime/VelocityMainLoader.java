package cn.mythicland.mythiclogin.velocity.runtime;

import cn.mythicland.mythiclogin.velocity.MythicLoginVelocity;
import cn.mythicland.mythiclogin.velocity.PluginLoader;

public final class VelocityMainLoader {
    private final MythicLoginVelocity plugin;

    public VelocityMainLoader(MythicLoginVelocity plugin) {
        this.plugin = plugin;
    }

    public void load() {
    }

    public void enable() {
        PluginLoader.onEnable(this.plugin);
    }

    public void reload() {
        throw new UnsupportedOperationException("Velocity runtime reload is not supported");
    }

    public void unload() {
        PluginLoader.onDisable();
    }
}
