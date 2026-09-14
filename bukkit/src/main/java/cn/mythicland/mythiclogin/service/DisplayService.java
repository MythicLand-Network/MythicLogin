package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.model.DisplaySettings;
import cn.mythicland.mythiclogin.model.AuthState;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.protocol.PacketDisplayAdapter;
import cn.mythicland.mythiclogin.util.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DisplayService {

    private static final int PERSISTENT_TITLE_FADE_IN = 0;
    private static final int PERSISTENT_TITLE_STAY = 999999;
    private static final int PERSISTENT_TITLE_FADE_OUT = 0;

    private final JavaPlugin plugin;
    private final PacketDisplayAdapter packetDisplayAdapter;
    private final ConfigManager configManager;
    private final MessageService messageService;
    private final Map<UUID, Long> nextCountdownSubtitleAt = new ConcurrentHashMap<>();
    private final Map<UUID, CountdownTitleState> countdownTitles = new ConcurrentHashMap<>();
    private final Map<UUID, Long> failureTitleUntil = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> failureRestoreTasks = new ConcurrentHashMap<>();

    public DisplayService(JavaPlugin plugin, ConfigManager configManager, MessageService messageService, PluginLogger pluginLogger) {
        this.plugin = plugin;
        this.packetDisplayAdapter = new PacketDisplayAdapter(plugin, pluginLogger);
        this.configManager = configManager;
        this.messageService = messageService;
    }

    public void reload() {
        nextCountdownSubtitleAt.clear();
        countdownTitles.clear();
        failureTitleUntil.clear();
        failureRestoreTasks.values().forEach(BukkitTask::cancel);
        failureRestoreTasks.clear();
    }

    public void clear(Player player) {
        nextCountdownSubtitleAt.remove(player.getUniqueId());
        countdownTitles.remove(player.getUniqueId());
        failureTitleUntil.remove(player.getUniqueId());
        BukkitTask task = failureRestoreTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void renderCountdown(Player player, AuthState state, long deadlineMillis) {
        DisplaySettings settings = configManager.getSettings().getDisplay();
        long remainingMillis = Math.max(0L, deadlineMillis - System.currentTimeMillis());
        int seconds = (int) Math.ceil(remainingMillis / 1000.0D);

        String text = state == AuthState.UNREGISTERED ? settings.getActionBarRegisterText() : settings.getActionBarLoginText();
        packetDisplayAdapter.sendActionBar(player, messageService.format(text, seconds));
        renderCountdownTitle(player, state, settings, player.getName(), seconds);
    }

    public void showSuccessTitle(Player player, AuthState previousState) {
        DisplaySettings settings = configManager.getSettings().getDisplay();
        String title = previousState == AuthState.UNREGISTERED ? settings.getRegisterTitle() : settings.getLoginTitle();
        String subtitle = previousState == AuthState.UNREGISTERED ? settings.getRegisterSubtitle() : settings.getLoginSubtitle();
        packetDisplayAdapter.sendTitle(
            player,
            messageService.format(title, player.getName()),
            messageService.format(subtitle, player.getName()),
            settings.getTitleFadeIn(),
            settings.getTitleStay(),
            settings.getTitleFadeOut()
        );
    }

    public void showAutoLoginTitle(Player player) {
        DisplaySettings settings = configManager.getSettings().getDisplay();
        packetDisplayAdapter.sendTitle(
            player,
            messageService.format(settings.getAutoLoginTitle(), player.getName()),
            messageService.format(settings.getAutoLoginSubtitle(), player.getName()),
            settings.getTitleFadeIn(),
            settings.getTitleStay(),
            settings.getTitleFadeOut()
        );
    }

    public void showLoginFailureTitle(Player player) {
        DisplaySettings settings = configManager.getSettings().getDisplay();
        packetDisplayAdapter.sendTitle(
            player,
            messageService.format(settings.getLoginFailureTitle(), player.getName()),
            messageService.format(settings.getLoginFailureSubtitle(), player.getName()),
            settings.getLoginFailureFadeIn(),
            settings.getLoginFailureStay(),
            settings.getLoginFailureFadeOut()
        );
        long restoreDelayTicks = Math.max(1L, settings.getLoginFailureRestoreDelayTicks());
        long restoreAt = System.currentTimeMillis() + restoreDelayTicks * 50L;
        failureTitleUntil.put(player.getUniqueId(), restoreAt);
        BukkitTask previous = failureRestoreTasks.remove(player.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            failureRestoreTasks.remove(player.getUniqueId());
            failureTitleUntil.remove(player.getUniqueId());
            countdownTitles.remove(player.getUniqueId());
            nextCountdownSubtitleAt.remove(player.getUniqueId());
            if (player.isOnline()) {
                restoreLoginPrompt(player);
            }
        }, restoreDelayTicks);
        failureRestoreTasks.put(player.getUniqueId(), task);
    }

    private void restoreLoginPrompt(Player player) {
        DisplaySettings settings = configManager.getSettings().getDisplay();
        String title = messageService.format(settings.getCountdownLoginTitle(), player.getName());
        String subtitle = messageService.format(settings.getCountdownLoginSubtitle(), player.getName());
        packetDisplayAdapter.sendTitleTimes(player, PERSISTENT_TITLE_FADE_IN, PERSISTENT_TITLE_STAY, PERSISTENT_TITLE_FADE_OUT);
        packetDisplayAdapter.sendTitleText(player, title);
        packetDisplayAdapter.sendSubtitleText(player, subtitle);
        long now = System.currentTimeMillis();
        long intervalMillis = Math.max(1L, settings.getCountdownSubtitleIntervalTicks()) * 50L;
        nextCountdownSubtitleAt.put(player.getUniqueId(), now + intervalMillis);
        countdownTitles.put(player.getUniqueId(), new CountdownTitleState(title, subtitle, true));
    }

    private void renderCountdownTitle(Player player, AuthState state, DisplaySettings settings, String playerName, int seconds) {
        long blockedUntil = failureTitleUntil.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() < blockedUntil) {
            return;
        }
        long now = System.currentTimeMillis();
        long nextSubtitleAt = nextCountdownSubtitleAt.getOrDefault(player.getUniqueId(), 0L);
        boolean shouldSendSubtitle = now >= nextSubtitleAt;
        CountdownTitleState previous = countdownTitles.get(player.getUniqueId());
        String title = state == AuthState.UNREGISTERED ? settings.getCountdownRegisterTitle() : settings.getCountdownLoginTitle();
        String subtitle = state == AuthState.UNREGISTERED ? settings.getCountdownRegisterSubtitle() : settings.getCountdownLoginSubtitle();
        String renderedTitle = messageService.format(title, seconds, playerName);
        String renderedSubtitle = messageService.format(subtitle, seconds, playerName);
        boolean shouldSendTitle = previous == null || !renderedTitle.equals(previous.title) || !previous.persistent;
        if (!shouldSendTitle && !shouldSendSubtitle) {
            return;
        }

        if (shouldSendTitle) {
            packetDisplayAdapter.sendTitleTimes(player, PERSISTENT_TITLE_FADE_IN, PERSISTENT_TITLE_STAY, PERSISTENT_TITLE_FADE_OUT);
            packetDisplayAdapter.sendTitleText(player, renderedTitle);
        }
        if (shouldSendSubtitle) {
            if (shouldSendTitle) {
                packetDisplayAdapter.sendTitleText(player, renderedTitle);
            }
            packetDisplayAdapter.sendSubtitleText(player, renderedSubtitle);
            long intervalMillis = Math.max(1L, settings.getCountdownSubtitleIntervalTicks()) * 50L;
            nextCountdownSubtitleAt.put(player.getUniqueId(), now + intervalMillis);
        }
        countdownTitles.put(player.getUniqueId(), new CountdownTitleState(renderedTitle, renderedSubtitle, true));
    }

    private static final class CountdownTitleState {
        private final String title;
        private final String subtitle;
        private final boolean persistent;

        private CountdownTitleState(String title, String subtitle, boolean persistent) {
            this.title = title;
            this.subtitle = subtitle;
            this.persistent = persistent;
        }
    }
}
