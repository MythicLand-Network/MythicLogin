package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RestrictionService {

    private final JavaPlugin plugin;
    private final AuthSessionManager authSessionManager;
    private final Map<UUID, RestrictedState> restrictedStates = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> restrictionWarmups = new ConcurrentHashMap<>();
    private BukkitTask restrictionTask;

    public RestrictionService(JavaPlugin plugin, ConfigManager configManager, AuthSessionManager authSessionManager) {
        this.plugin = plugin;
        this.authSessionManager = authSessionManager;
    }

    public void start() {
        startRestrictionTask();
    }

    public void reload() {
        startRestrictionTask();
    }

    public void applyRestrictedState(Player player) {
        restrictedStates.computeIfAbsent(player.getUniqueId(), ignored -> RestrictedState.capture(player));
        applyRestrictionAttributes(player, true);
        startRestrictionWarmup(player);
    }

    public void clearRestrictedState(Player player) {
        cancelRestrictionWarmup(player.getUniqueId());
        RestrictedState state = restrictedStates.remove(player.getUniqueId());
        clearRestrictionEffects(player, PotionEffectType.BLINDNESS, PotionEffectType.JUMP);
        if (state == null) {
            player.setWalkSpeed(0.2F);
            player.setFlySpeed(0.1F);
            return;
        }
        player.setAllowFlight(state.allowFlight);
        player.setWalkSpeed(state.walkSpeed);
        player.setFlySpeed(state.flySpeed);
    }

    public void shutdown() {
        if (restrictionTask != null) {
            restrictionTask.cancel();
        }
        restrictionWarmups.values().forEach(BukkitTask::cancel);
        restrictionWarmups.clear();
        Bukkit.getOnlinePlayers().forEach(this::clearRestrictedState);
        restrictedStates.clear();
    }

    private void startRestrictionTask() {
        if (restrictionTask != null) {
            restrictionTask.cancel();
        }
        restrictionTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRestrictions, 10L, 10L);
    }

    private void tickRestrictions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!authSessionManager.shouldRestrict(player)) {
                continue;
            }
            applyRestrictionAttributes(player, false);
        }
    }

    private void startRestrictionWarmup(Player player) {
        UUID uniqueId = player.getUniqueId();
        cancelRestrictionWarmup(uniqueId);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int remainingRuns = 5;

            @Override
            public void run() {
                Player onlinePlayer = Bukkit.getPlayer(uniqueId);
                if (onlinePlayer == null || !onlinePlayer.isOnline() || !authSessionManager.shouldRestrict(onlinePlayer) || remainingRuns-- <= 0) {
                    cancelRestrictionWarmup(uniqueId);
                    return;
                }
                applyRestrictionAttributes(onlinePlayer, true);
            }
        }, 1L, 1L);
        restrictionWarmups.put(uniqueId, task);
    }

    private void cancelRestrictionWarmup(UUID uniqueId) {
        BukkitTask task = restrictionWarmups.remove(uniqueId);
        if (task != null) {
            task.cancel();
        }
    }

    private void applyRestrictionAttributes(Player player, boolean forcePotionRefresh) {
        player.setWalkSpeed(0.0F);
        player.setFlySpeed(0.0F);
        player.setAllowFlight(false);
        player.setSprinting(false);
        if (player.isFlying()) {
            player.setFlying(false);
        }

        if (forcePotionRefresh || findPotionEffect(player, PotionEffectType.BLINDNESS) == null) {
            clearRestrictionEffects(player, PotionEffectType.BLINDNESS);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false), true);
        }
        if (forcePotionRefresh || findPotionEffect(player, PotionEffectType.JUMP) == null) {
            clearRestrictionEffects(player, PotionEffectType.JUMP);
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 200, false, false), true);
        }
    }

    private void clearRestrictionEffects(Player player, PotionEffectType... effectTypes) {
        for (PotionEffectType effectType : effectTypes) {
            player.removePotionEffect(effectType);
        }
    }

    private PotionEffect findPotionEffect(Player player, PotionEffectType type) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(type)) {
                return effect;
            }
        }
        return null;
    }

    private static final class RestrictedState {
        private final float walkSpeed;
        private final float flySpeed;
        private final boolean allowFlight;

        private RestrictedState(float walkSpeed, float flySpeed, boolean allowFlight) {
            this.walkSpeed = walkSpeed;
            this.flySpeed = flySpeed;
            this.allowFlight = allowFlight;
        }

        private static RestrictedState capture(Player player) {
            return new RestrictedState(player.getWalkSpeed(), player.getFlySpeed(), player.getAllowFlight());
        }
    }
}
