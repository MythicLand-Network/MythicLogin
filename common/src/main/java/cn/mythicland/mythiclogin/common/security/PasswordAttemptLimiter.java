package cn.mythicland.mythiclogin.common.security;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

public final class PasswordAttemptLimiter {
    private static final long WINDOW_NANOS = Duration.ofMinutes(1).toNanos();
    private static final int ACCOUNT_LIMIT = 5;
    private static final int ADDRESS_LIMIT = 20;
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_IN_FLIGHT = 2;

    private final Map<String, Window> accounts = new HashMap<>();
    private final Map<String, Window> addresses = new HashMap<>();
    private final LongSupplier clock;
    private int inFlight;

    public PasswordAttemptLimiter() {
        this(System::nanoTime);
    }

    PasswordAttemptLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    public synchronized boolean tryAcquire(String account, String address) {
        long now = clock.getAsLong();
        String name = account.toLowerCase(Locale.ROOT);
        String source = address == null ? "" : address;
        Window accountWindow = currentWindow(accounts, name, now);
        Window addressWindow = currentWindow(addresses, source, now);
        if (inFlight >= MAX_IN_FLIGHT || accountWindow != null && (accountWindow.inFlight || accountWindow.attempts >= ACCOUNT_LIMIT)
                || addressWindow != null && addressWindow.attempts >= ADDRESS_LIMIT) return false;
        if (accountWindow == null && !hasCapacity(accounts, now) || addressWindow == null && !hasCapacity(addresses, now)) return false;
        if (accountWindow == null) {
            accountWindow = new Window(now);
            accounts.put(name, accountWindow);
        }
        if (addressWindow == null) {
            addressWindow = new Window(now);
            addresses.put(source, addressWindow);
        }
        accountWindow.attempts++;
        addressWindow.attempts++;
        accountWindow.inFlight = true;
        inFlight++;
        return true;
    }

    public synchronized void release(String account) {
        Window window = accounts.get(account.toLowerCase(Locale.ROOT));
        if (window == null || !window.inFlight) throw new IllegalStateException("No password attempt in progress");
        window.inFlight = false;
        inFlight--;
    }

    private Window currentWindow(Map<String, Window> windows, String key, long now) {
        Window window = windows.get(key);
        if (window != null && !window.inFlight && now - window.startedAt >= WINDOW_NANOS) {
            windows.remove(key);
            return null;
        }
        return window;
    }

    private boolean hasCapacity(Map<String, Window> windows, long now) {
        if (windows.size() < MAX_ENTRIES) return true;
        windows.values().removeIf(window -> !window.inFlight && now - window.startedAt >= WINDOW_NANOS);
        return windows.size() < MAX_ENTRIES;
    }

    private static final class Window {
        private final long startedAt;
        private int attempts;
        private boolean inFlight;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
