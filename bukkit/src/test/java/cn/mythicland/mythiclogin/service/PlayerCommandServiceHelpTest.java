package cn.mythicland.mythiclogin.service;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class PlayerCommandServiceHelpTest {
    @Test
    void routesSensitiveEmailCommandsThroughEmailService() {
        Player player = mock(Player.class);
        AuthService authService = mock(AuthService.class);
        EmailAuthService emailAuthService = mock(EmailAuthService.class);
        MessageService messageService = mock(MessageService.class);
        when(emailAuthService.requestBinding(player, "player@example.com"))
                .thenReturn(CompletableFuture.completedFuture("sent"));
        when(emailAuthService.verifyBinding(player, "123456"))
                .thenReturn(CompletableFuture.completedFuture("bound"));
        when(emailAuthService.requestPasswordReset(player))
                .thenReturn(CompletableFuture.completedFuture("sent"));
        when(emailAuthService.resetPassword(player, "123456", "secret", "secret"))
                .thenReturn(CompletableFuture.completedFuture("reset"));
        PlayerCommandService service = new PlayerCommandService(
                authService,
                emailAuthService,
                messageService,
                () -> {
                }
        );

        service.handle(player, "/bind email player@example.com");
        service.handle(player, "/bind code 123456");
        service.handle(player, "/fp");
        service.handle(player, "/forgotpassword 123456 secret secret");

        verify(emailAuthService).requestBinding(player, "player@example.com");
        verify(emailAuthService).verifyBinding(player, "123456");
        verify(emailAuthService).requestPasswordReset(player);
        verify(emailAuthService).resetPassword(player, "123456", "secret", "secret");
        verify(authService, times(4)).sendMessageWhenComplete(
                eq(player),
                any()
        );
    }

}
