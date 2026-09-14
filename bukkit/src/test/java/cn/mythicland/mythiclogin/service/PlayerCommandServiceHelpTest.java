package cn.mythicland.mythiclogin.service;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

final class PlayerCommandServiceHelpTest {
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
        verify(sender, times(8)).sendMessage(messages.capture());
        assertEquals(
                List.of(
                        "§4> §c/login <密码>",
                        "§4> §c/register <密码> <密码>",
                        "§4> §c/logout",
                        "§4> §c/cp <新密码> <重复新密码>",
                        "§4> §c/mlg help",
                        "§4> §c/mlg reload",
                        "§4> §c/mlg password <玩家名> <新密码>",
                        "§4> §c/mlg unregister <玩家名>"
                ),
                messages.getAllValues()
        );
    }

    private PlayerCommandService service() {
        return new PlayerCommandService(
                mock(AuthService.class),
                mock(MessageService.class),
                () -> {
                }
        );
    }
}
