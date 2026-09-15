package cn.mythicland.mythiclogin.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.config.key.MessageKey;

@InjectComponent
public final class StartupGateService {

    private final MessageService messageService;
    private volatile boolean ready;
    private volatile String kickMessage;

    public StartupGateService(MessageService messageService) {
        this.messageService = messageService;
        reload();
        markStarting();
    }

    public void reload() {
        kickMessage = messageService.get(MessageKey.SYSTEM_STARTING_UP_KICK);
    }

    public void markStarting() {
        ready = false;
    }

    public void markReady() {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    public String getKickMessage() {
        return kickMessage;
    }
}
