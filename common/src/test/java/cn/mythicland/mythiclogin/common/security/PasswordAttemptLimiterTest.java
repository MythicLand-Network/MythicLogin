package cn.mythicland.mythiclogin.common.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PasswordAttemptLimiterTest {
    @Test
    void limitsAccountsAndAddressesAcrossReconnectsAndExpires() {
        AtomicLong time = new AtomicLong();
        PasswordAttemptLimiter limiter = new PasswordAttemptLimiter(time::get);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("Player", "ip1"));
            limiter.release("player");
        }
        assertFalse(limiter.tryAcquire("PLAYER", "ip2"));
        for (int i = 0; i < 15; i++) {
            String name = "other" + i;
            assertTrue(limiter.tryAcquire(name, "ip1"));
            limiter.release(name);
        }
        assertFalse(limiter.tryAcquire("another", "ip1"));
        time.addAndGet(Duration.ofMinutes(1).toNanos());
        assertTrue(limiter.tryAcquire("player", "ip1"));
        limiter.release("player");
    }

    @Test
    void boundsConcurrentWorkAndKeepsInFlightAccountAcrossWindowExpiry() {
        AtomicLong time = new AtomicLong();
        PasswordAttemptLimiter limiter = new PasswordAttemptLimiter(time::get);
        assertTrue(limiter.tryAcquire("a", "ip1"));
        assertFalse(limiter.tryAcquire("A", "ip2"));
        assertTrue(limiter.tryAcquire("b", "ip2"));
        assertFalse(limiter.tryAcquire("c", "ip3"));
        time.addAndGet(Duration.ofMinutes(1).toNanos());
        assertFalse(limiter.tryAcquire("a", "ip1"));
        limiter.release("a");
        assertTrue(limiter.tryAcquire("c", "ip3"));
        limiter.release("b");
        limiter.release("c");
        assertTrue(limiter.tryAcquire("a", "ip1"));
        limiter.release("a");
    }

    @Test
    void simultaneousRequestsCannotBypassInFlightLimit() throws Exception {
        PasswordAttemptLimiter limiter = new PasswordAttemptLimiter();
        AtomicInteger accepted = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(12);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(12)) {
            for (int i = 0; i < 12; i++) {
                int id = i;
                executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    if (limiter.tryAcquire("player" + id, "ip" + id)) accepted.incrementAndGet();
                    return null;
                });
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(2, accepted.get());
    }

    @Test
    void failsClosedAtCapacityAndRecoversAfterExpiry() {
        AtomicLong time = new AtomicLong();
        PasswordAttemptLimiter limiter = new PasswordAttemptLimiter(time::get);
        for (int i = 0; i < 4096; i++) {
            String name = "player" + i;
            assertTrue(limiter.tryAcquire(name, "ip" + i));
            limiter.release(name);
        }
        assertFalse(limiter.tryAcquire("new", "new-ip"));
        time.addAndGet(Duration.ofMinutes(1).toNanos());
        assertTrue(limiter.tryAcquire("new", "new-ip"));
        limiter.release("new");
    }
}
