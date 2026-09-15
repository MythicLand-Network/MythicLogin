package cn.mythicland.mythiclogin.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.model.AuthState;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@InjectComponent
public final class AuthSessionManager {

    private final Map<UUID, SessionContext> sessions = new ConcurrentHashMap<>();

    public void prepareJoin(Player player) {
        sessions.put(player.getUniqueId(), SessionContext.loading());
    }

    public void prepareJoinIfAbsent(Player player) {
        sessions.putIfAbsent(player.getUniqueId(), SessionContext.loading());
    }

    public void setPending(Player player, AuthState state, Account account, int timeoutSeconds) {
        long deadlineMillis = System.currentTimeMillis() + timeoutSeconds * 1000L;
        sessions.put(player.getUniqueId(), new SessionContext(state, account, deadlineMillis));
    }

    public void authenticate(Player player, Account account) {
        sessions.put(player.getUniqueId(), SessionContext.authenticated(account));
    }

    public void replaceAccount(Player player, Account account) {
        sessions.computeIfPresent(player.getUniqueId(), (ignored, context) ->
                new SessionContext(context.state, account, context.deadlineMillis));
    }

    public void extendDeadline(Player player, long deadlineMillis) {
        sessions.computeIfPresent(player.getUniqueId(), (ignored, context) ->
                new SessionContext(context.state, context.account, Math.max(context.deadlineMillis, deadlineMillis)));
    }

    public SessionContext remove(Player player) {
        return sessions.remove(player.getUniqueId());
    }

    public SessionContext getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean isAuthenticated(Player player) {
        SessionContext context = sessions.get(player.getUniqueId());
        return context != null && context.getState() == AuthState.AUTHENTICATED;
    }

    public boolean shouldRestrict(Player player) {
        SessionContext context = sessions.get(player.getUniqueId());
        return context == null || context.getState() != AuthState.AUTHENTICATED;
    }

    public void clear() {
        sessions.clear();
    }

    public static final class SessionContext {
        private final AuthState state;
        private final Account account;
        private final long deadlineMillis;

        private SessionContext(AuthState state, Account account, long deadlineMillis) {
            this.state = state;
            this.account = account;
            this.deadlineMillis = deadlineMillis;
        }

        public static SessionContext loading() {
            return new SessionContext(AuthState.LOADING, null, 0L);
        }

        public static SessionContext authenticated(Account account) {
            return new SessionContext(AuthState.AUTHENTICATED, account, 0L);
        }

        public AuthState getState() {
            return state;
        }

        public Account getAccount() {
            return account;
        }

        public long getDeadlineMillis() {
            return deadlineMillis;
        }
    }
}
