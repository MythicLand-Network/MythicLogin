package cn.mythicland.mythiclogin.email;

import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import com.resend.core.net.RequestOptions;
import com.resend.services.emails.model.CreateEmailOptions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResendEmailGatewayTest {

    @Test
    void shouldBuildFormalEmailChangedNotification() throws Exception {
        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getEmailSettings()).thenReturn(new EmailSettings(
                "re_test", "MythicLogin <login@example.com>", 6, 5, 60, 5
        ));
        AtomicReference<CreateEmailOptions> capturedEmail = new AtomicReference<>();
        AtomicReference<RequestOptions> capturedRequest = new AtomicReference<>();
        ResendEmailGateway gateway = new ResendEmailGateway(
                configManager,
                (ignored, email, request) -> {
                    capturedEmail.set(email);
                    capturedRequest.set(request);
                }
        );

        gateway.send(new AccountNotificationEmail(
                AccountNotificationType.EMAIL_CHANGED,
                "old@example.com",
                "Player",
                "mythiclogin/email-changed/1"
        ));

        assertEquals("old@example.com", capturedEmail.get().getTo().get(0));
        assertEquals("【MythicLand Network】邮箱更换通知", capturedEmail.get().getSubject());
        assertTrue(capturedEmail.get().getText().contains("不再接收验证码及安全通知"));
        assertTrue(capturedEmail.get().getHtml().contains("如非本人操作"));
        assertEquals("mythiclogin/email-changed/1", capturedRequest.get().getIdempotencyKey());
    }

    @Test
    void shouldBuildChineseVerificationEmailWithIdempotencyKey() throws Exception {
        ConfigManager configManager = mock(ConfigManager.class);
        EmailSettings settings = new EmailSettings(
                "re_test",
                "MythicLogin <login@example.com>",
                6,
                5,
                60,
                5
        );
        when(configManager.getEmailSettings()).thenReturn(settings);
        AtomicReference<CreateEmailOptions> capturedEmail = new AtomicReference<>();
        AtomicReference<RequestOptions> capturedRequest = new AtomicReference<>();
        ResendEmailGateway gateway = new ResendEmailGateway(
                configManager,
                (ignored, email, request) -> {
                    capturedEmail.set(email);
                    capturedRequest.set(request);
                }
        );

        gateway.send(new VerificationEmail(
                VerificationPurpose.RESET_PASSWORD,
                "player@example.com",
                "Player",
                "123456",
                5,
                "mythiclogin/reset/1"
        ));

        assertEquals("MythicLogin <login@example.com>", capturedEmail.get().getFrom());
        assertEquals("player@example.com", capturedEmail.get().getTo().get(0));
        assertEquals("【MythicLand Network】密码重置验证码", capturedEmail.get().getSubject());
        assertTrue(capturedEmail.get().getText().contains("123456"));
        assertTrue(capturedEmail.get().getText().contains("密码重置"));
        assertTrue(capturedEmail.get().getText().contains("5 分钟"));
        assertTrue(capturedEmail.get().getText().contains("如非本人操作, 请忽略此邮件"));
        assertTrue(capturedEmail.get().getHtml().contains("123456"));
        assertTrue(capturedEmail.get().getHtml().contains("密码重置"));
        assertTrue(capturedEmail.get().getHtml().contains("此邮件由系统自动发送, 请勿回复"));
        assertEquals("mythiclogin/reset/1", capturedRequest.get().getIdempotencyKey());
    }
}
