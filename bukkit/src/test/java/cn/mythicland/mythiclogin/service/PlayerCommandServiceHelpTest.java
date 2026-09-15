package cn.mythicland.mythiclogin.service;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class PlayerCommandServiceHelpTest {
    @Test
    void sendsUsageHintThroughTheInGameCommandPath() {
        Player player = mock(Player.class);
        PlayerCommandService service = service();

        service.handle(player, "/mlg");

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(player, times(2)).sendMessage(messages.capture());
        assertEquals(
                List.of(
                        "§c用法: /mlg <子命令>",
                        "§c使用 /mlg help 查看所有命令。"
                ),
                messages.getAllValues()
        );
    }

    @Test
    void sendsUsageHintForRootCommandWithoutArguments() {
        CommandSender sender = mock(CommandSender.class);
        PlayerCommandService service = service();

        service.sendRootUsage(sender, "mythiclogin");

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(sender, times(2)).sendMessage(messages.capture());
        assertEquals(
                List.of(
                        "§c用法: /mythiclogin <子命令>",
                        "§c使用 /mythiclogin help 查看所有命令。"
                ),
                messages.getAllValues()
        );
    }

    @Test
    void sendsThePitStyleHelpForHelpCommand() {
        CommandSender sender = mock(CommandSender.class);
        PlayerCommandService service = service();

        service.sendHelp(sender);

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(sender, times(11)).sendMessage(messages.capture());
        assertEquals(
                List.of(
                        "§4> §c/login <密码>",
                        "§4> §c/register <密码> <密码>",
                        "§4> §c/logout",
                        "§4> §c/cp <新密码> <重复新密码>",
                        "§4> §c/bind email <邮箱>",
                        "§4> §c/bind code <验证码>",
                        "§4> §c/fp [验证码] [新密码] [重复密码]",
                        "§4> §c/mlg help",
                        "§4> §c/mlg reload",
                        "§4> §c/mlg password <玩家名> <新密码>",
                        "§4> §c/mlg unregister <玩家名>"
                ),
                messages.getAllValues()
        );
    }

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

    private PlayerCommandService service() {
        return new PlayerCommandService(
                mock(AuthService.class),
                mock(EmailAuthService.class),
                mock(MessageService.class),
                () -> {
                }
        );
    }
}
