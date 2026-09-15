package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.email.VerificationPurpose;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public final class VerificationCodeService {
    private final SecureRandom secureRandom;
    private final LongSupplier currentTimeMillis;
    private final Map<VerificationKey, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();
    private final Map<VerificationKey, Long> lastSentAt = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();

    public VerificationCodeService() {
        this(new SecureRandom(), System::currentTimeMillis);
    }

    VerificationCodeService(SecureRandom secureRandom, LongSupplier currentTimeMillis) {
        this.secureRandom = secureRandom;
        this.currentTimeMillis = currentTimeMillis;
    }

    public Challenge createChallenge(
            UUID playerId,
            VerificationPurpose purpose,
            String targetEmail,
            EmailSettings settings
    ) {
        String code = generateCode(settings.getCodeLength());
        long expiresAtMillis = currentTimeMillis.getAsLong() + settings.getExpireMinutes() * 60_000L;
        PendingVerification pending = new PendingVerification(
                targetEmail,
                digest(code),
                expiresAtMillis,
                settings.getMaxAttempts(),
                0
        );
        return new Challenge(new VerificationKey(playerId, purpose), code, pending, generation.get());
    }

    public boolean activate(Challenge challenge) {
        if (challenge.generation() != generation.get()) return false;
        pendingVerifications.put(challenge.key(), challenge.pending());
        lastSentAt.put(challenge.key(), currentTimeMillis.getAsLong());
        return true;
    }

    public long remainingCooldownSeconds(
            UUID playerId,
            VerificationPurpose purpose,
            int cooldownSeconds
    ) {
        Long sentAtMillis = lastSentAt.get(new VerificationKey(playerId, purpose));
        if (sentAtMillis == null) return 0L;
        long remainingMillis = sentAtMillis + cooldownSeconds * 1000L - currentTimeMillis.getAsLong();
        if (remainingMillis <= 0L) {
            lastSentAt.remove(new VerificationKey(playerId, purpose), sentAtMillis);
            return 0L;
        }
        return (remainingMillis + 999L) / 1000L;
    }

    public boolean hasPending(UUID playerId, VerificationPurpose purpose) {
        return pendingVerifications.containsKey(new VerificationKey(playerId, purpose));
    }

    public VerificationResult verify(UUID playerId, VerificationPurpose purpose, String suppliedCode) {
        VerificationKey key = new VerificationKey(playerId, purpose);
        PendingVerification pending = pendingVerifications.get(key);
        if (pending == null) return VerificationResult.of(VerificationStatus.MISSING);
        if (currentTimeMillis.getAsLong() >= pending.expiresAtMillis()) {
            pendingVerifications.remove(key, pending);
            return VerificationResult.of(VerificationStatus.EXPIRED);
        }
        if (MessageDigest.isEqual(pending.codeDigest(), digest(suppliedCode.trim()))) {
            pendingVerifications.remove(key, pending);
            return new VerificationResult(VerificationStatus.VALID, pending.targetEmail());
        }
        int failedAttempts = pending.failedAttempts() + 1;
        if (failedAttempts >= pending.maxAttempts()) {
            pendingVerifications.remove(key, pending);
            return VerificationResult.of(VerificationStatus.EXHAUSTED);
        }
        pendingVerifications.replace(key, pending, pending.withFailedAttempts(failedAttempts));
        return VerificationResult.of(VerificationStatus.INVALID);
    }

    public void clear() {
        generation.incrementAndGet();
        pendingVerifications.clear();
        lastSentAt.clear();
    }

    private String generateCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    private byte[] digest(String code) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record Challenge(VerificationKey key, String code, PendingVerification pending, long generation) {
        public long expiresAtMillis() {
            return pending.expiresAtMillis();
        }
    }

    public record VerificationResult(VerificationStatus status, String targetEmail) {
        private static VerificationResult of(VerificationStatus status) {
            return new VerificationResult(status, null);
        }
    }

    public enum VerificationStatus {
        VALID,
        MISSING,
        EXPIRED,
        INVALID,
        EXHAUSTED
    }

    public record VerificationKey(UUID playerId, VerificationPurpose purpose) {
    }

    public record PendingVerification(
            String targetEmail,
            byte[] codeDigest,
            long expiresAtMillis,
            int maxAttempts,
            int failedAttempts
    ) {
        private PendingVerification withFailedAttempts(int attempts) {
            return new PendingVerification(targetEmail, codeDigest, expiresAtMillis, maxAttempts, attempts);
        }
    }
}
