package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.email.VerificationPurpose;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationCodeServiceTest {
    private final EmailSettings settings = new EmailSettings(
            "re_test",
            "MythicLogin <login@example.com>",
            6,
            5,
            60,
            2
    );

    @Test
    void shouldIssueVerifyAndConsumeCodeForOnePurpose() {
        AtomicLong clock = new AtomicLong(1_000L);
        VerificationCodeService service = new VerificationCodeService(new SecureRandom(), clock::get);
        UUID playerId = UUID.randomUUID();
        VerificationCodeService.Challenge challenge = service.createChallenge(
                playerId,
                VerificationPurpose.BIND_EMAIL,
                "player@example.com",
                settings
        );

        assertEquals(6, challenge.code().length());
        assertTrue(challenge.code().chars().allMatch(Character::isDigit));
        assertFalse(service.hasPending(playerId, VerificationPurpose.BIND_EMAIL));

        assertTrue(service.activate(challenge));

        assertTrue(service.hasPending(playerId, VerificationPurpose.BIND_EMAIL));
        assertEquals(60L, service.remainingCooldownSeconds(
                playerId,
                VerificationPurpose.BIND_EMAIL,
                settings.resendCooldownSeconds()
        ));
        assertEquals(
                VerificationCodeService.VerificationStatus.MISSING,
                service.verify(playerId, VerificationPurpose.RESET_PASSWORD, challenge.code()).status()
        );
        VerificationCodeService.VerificationResult verified = service.verify(
                playerId,
                VerificationPurpose.BIND_EMAIL,
                challenge.code()
        );
        assertEquals(VerificationCodeService.VerificationStatus.VALID, verified.status());
        assertEquals("player@example.com", verified.targetEmail());
        assertFalse(service.hasPending(playerId, VerificationPurpose.BIND_EMAIL));
    }

    @Test
    void shouldExpireAndExhaustVerificationCode() {
        AtomicLong clock = new AtomicLong(1_000L);
        VerificationCodeService service = new VerificationCodeService(new SecureRandom(), clock::get);
        UUID playerId = UUID.randomUUID();
        VerificationCodeService.Challenge first = service.createChallenge(
                playerId,
                VerificationPurpose.RESET_PASSWORD,
                "player@example.com",
                settings
        );
        assertTrue(service.activate(first));

        assertEquals(
                VerificationCodeService.VerificationStatus.INVALID,
                service.verify(playerId, VerificationPurpose.RESET_PASSWORD, "wrong").status()
        );
        assertEquals(
                VerificationCodeService.VerificationStatus.EXHAUSTED,
                service.verify(playerId, VerificationPurpose.RESET_PASSWORD, "wrong").status()
        );

        VerificationCodeService.Challenge second = service.createChallenge(
                playerId,
                VerificationPurpose.RESET_PASSWORD,
                "player@example.com",
                settings
        );
        assertTrue(service.activate(second));
        clock.set(second.expiresAtMillis());

        assertEquals(
                VerificationCodeService.VerificationStatus.EXPIRED,
                service.verify(playerId, VerificationPurpose.RESET_PASSWORD, second.code()).status()
        );
    }

    @Test
    void shouldNotActivateChallengeCreatedBeforeClear() {
        VerificationCodeService service = new VerificationCodeService();
        UUID playerId = UUID.randomUUID();
        VerificationCodeService.Challenge challenge = service.createChallenge(
                playerId,
                VerificationPurpose.BIND_EMAIL,
                "player@example.com",
                settings
        );

        service.clear();
        assertFalse(service.activate(challenge));

        assertFalse(service.hasPending(playerId, VerificationPurpose.BIND_EMAIL));
    }
}
