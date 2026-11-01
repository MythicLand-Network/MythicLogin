package cn.mythicland.mythiclogin.listener;

import cn.mythicland.framework.bukkit.component.ListenerComponent;
import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.service.AuthService;
import java.util.Optional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

@InjectComponent
@ListenerComponent
public final class CaseSensitiveAccountListener implements Listener {

    private final AuthService authService;

    public CaseSensitiveAccountListener(AuthService authService) {
        this.authService = authService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        Optional<String> caseMismatchRealName = authService.findCaseMismatchRealName(event.getName());
        if (caseMismatchRealName.isEmpty()) return;

        event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                authService.caseMismatchMessage(caseMismatchRealName.get())
        );
    }
}
