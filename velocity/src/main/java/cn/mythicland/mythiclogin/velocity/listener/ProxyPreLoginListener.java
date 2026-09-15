package cn.mythicland.mythiclogin.velocity.listener;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import cn.mythicland.mythiclogin.velocity.service.MessageService;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;

import java.util.Optional;

@InjectComponent
public final class ProxyPreLoginListener {

    private final IProxyRestrictionManager proxyRestrictionManager;

    public ProxyPreLoginListener(IProxyRestrictionManager proxyRestrictionManager) {
        this.proxyRestrictionManager = proxyRestrictionManager;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        String username = event.getUsername();
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        Optional<String> kickMessage = proxyRestrictionManager.findCaseMismatchKickMessage(username);
        if (!kickMessage.isPresent()) {
            return;
        }

        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
            MessageService.deserialize(kickMessage.get())
        ));
    }
}
