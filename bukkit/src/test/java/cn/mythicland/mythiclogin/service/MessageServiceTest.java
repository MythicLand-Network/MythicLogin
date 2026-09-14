package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.key.MessageKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageServiceTest {

    @Test
    void shouldRenderHardcodedMessageAndPlaceholders() {
        MessageService messageService = new MessageService();

        assertEquals("\u00A7a登录成功, 欢迎回来。", messageService.get(MessageKey.AUTH_LOGIN_SUCCESS));
        assertEquals(
                "\u00A7c检测到已有账号, 请使用正确的游戏账户 \u00A7fTester \u00A7c登录。",
                messageService.get(MessageKey.AUTH_CASE_MISMATCH, "Tester")
        );
    }
}
