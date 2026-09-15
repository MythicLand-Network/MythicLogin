package cn.mythicland.mythiclogin.listener;

import cn.mythicland.framework.bukkit.ListenerComponent;
import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.service.AuthService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Optional;

@InjectComponent
@ListenerComponent
public final class CaseSensitiveAccountListener implements Listener {

    private final AuthService authService;

    public CaseSensitiveAccountListener(AuthService authService) {
        this.authService = authService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        Optional<String> caseMismatchRealName = authService.findCaseMismatchRealName(event.getName());
        if (!caseMismatchRealName.isPresent()) {
            return;
        }

        event.disallow(
            AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
            authService.caseMismatchMessage(caseMismatchRealName.get())
        );
    }
}
