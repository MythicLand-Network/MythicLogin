package cn.mythicland.mythiclogin;

import cn.mythicland.framework.command.CommandRouteCompiler;
import cn.mythicland.framework.component.ComponentContainer;
import cn.mythicland.framework.component.ComponentResolver;
import cn.mythicland.mythiclogin.email.EmailGateway;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.protocol.ProtocolGatekeeper;
import cn.mythicland.mythiclogin.service.AuthService;
import cn.mythicland.mythiclogin.service.PlayerCommandService;
import cn.mythicland.mythiclogin.service.VerificationCodeService;
import cn.mythicland.mythiclogin.util.PluginLogger;
import com.comphenix.protocol.ProtocolManager;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ComponentGraphTest {

    @Test
    void shouldResolveBukkitComponentGraph() {
        ComponentContainer components = new ComponentContainer();
        MythicLogin plugin = mock(MythicLogin.class);
        components.bind(plugin);
        components.bind(new PluginLogger(Logger.getLogger("ComponentGraphTest"), "Test"));
        components.bind(mock(ConfigManager.class));
        components.bind(mock(DataManager.class));
        components.bind(mock(ProtocolManager.class));
        components.bind(VerificationCodeService.class, new VerificationCodeService());
        components.bind(EmailGateway.class, mock(EmailGateway.class));
        components.bind(Runnable.class, (Runnable) () -> {
        });
        components.bind(Consumer.class, (Consumer<Runnable>) Runnable::run);
        components.bind(ComponentResolver.class, components);
        components.bind(new CommandRouteCompiler(components));

        components.scan(
                "cn.mythicland.mythiclogin",
                type -> !type.getName().startsWith("cn.mythicland.mythiclogin.velocity.")
        );

        AuthService authService = components.resolve(AuthService.class);
        PlayerCommandService commandService = components.resolve(PlayerCommandService.class);
        assertTrue(components.managedTypes().stream()
                .noneMatch(type -> type.getName().startsWith("cn.mythicland.mythiclogin.velocity.")));
        assertSame(authService, components.resolve(AuthService.class));
        assertSame(commandService, components.resolve(PlayerCommandService.class));
        assertSame(components.resolve(ProtocolGatekeeper.class), components.resolve(ProtocolGatekeeper.class));
    }
}
