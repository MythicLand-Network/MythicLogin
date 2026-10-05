package cn.mythicland.mythiclogin.velocity;

import cn.mythicland.framework.component.ComponentContainer;
import cn.mythicland.framework.minecraft.MinecraftProfiles;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.velocity.config.PluginSettings;
import cn.mythicland.mythiclogin.velocity.listener.ProxyCommandListener;
import cn.mythicland.mythiclogin.velocity.listener.ProxyPreLoginListener;
import cn.mythicland.mythiclogin.velocity.listener.ProxySessionListener;
import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;
import com.velocitypowered.api.proxy.ProxyServer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class ComponentGraphTest {

    @Test
    void shouldResolveVelocityComponentGraph() {
        ComponentContainer components = new ComponentContainer();
        components.bind(mock(PluginSettings.class));
        components.bind(mock(ProxyServer.class));
        components.bind(mock(MinecraftProfiles.class));
        components.bind(mock(AccountRepository.class));
        components.bind(new PluginLogger(mock(Logger.class), "Test"));

        components.scan("cn.mythicland.mythiclogin.velocity");

        IProxyRestrictionManager restrictionManager = components.resolve(IProxyRestrictionManager.class);
        assertSame(restrictionManager, components.resolve(IProxyRestrictionManager.class));
        assertSame(components.resolve(ProxyPreLoginListener.class), components.resolve(ProxyPreLoginListener.class));
        assertSame(components.resolve(ProxyCommandListener.class), components.resolve(ProxyCommandListener.class));
        assertSame(components.resolve(ProxySessionListener.class), components.resolve(ProxySessionListener.class));
    }
}
