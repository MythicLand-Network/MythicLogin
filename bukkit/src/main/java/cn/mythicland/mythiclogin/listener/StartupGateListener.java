package cn.mythicland.mythiclogin.listener;

import cn.mythicland.framework.bukkit.ListenerComponent;
import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.service.StartupGateService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

@InjectComponent
@ListenerComponent
public final class StartupGateListener implements Listener {

    private final StartupGateService startupGateService;

    public StartupGateListener(StartupGateService startupGateService) {
        this.startupGateService = startupGateService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        if (startupGateService.isReady()) return;
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, startupGateService.getKickMessage());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;
        if (startupGateService.isReady()) return;
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, startupGateService.getKickMessage());
    }
}
