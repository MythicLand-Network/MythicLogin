package cn.mythicland.mythiclogin.common.restriction;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandListMatcherTest {

    @Test
    void shouldAllowOnlyWhitelistedCommands() {
        CommandListMatcher matcher = new CommandListMatcher(
            CommandAccessMode.WHITELIST,
            Arrays.asList("/login", "register"),
            Arrays.asList("/server")
        );

        assertFalse(matcher.shouldBlock("/login"));
        assertFalse(matcher.shouldBlock("/register secret secret"));
        assertTrue(matcher.shouldBlock("/server hub"));
        assertTrue(matcher.shouldBlock("/glist"));
    }

    @Test
    void shouldBlockOnlyBlacklistedCommands() {
        CommandListMatcher matcher = new CommandListMatcher(
            CommandAccessMode.BLACKLIST,
            Arrays.asList("/login"),
            Arrays.asList("server", "/send", "/glist")
        );

        assertTrue(matcher.shouldBlock("/server"));
        assertTrue(matcher.shouldBlock("/SeNd lobby player"));
        assertFalse(matcher.shouldBlock("/login"));
        assertFalse(matcher.shouldBlock("/msg test"));
    }
}
