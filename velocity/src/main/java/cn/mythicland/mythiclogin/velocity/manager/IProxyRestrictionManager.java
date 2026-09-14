package cn.mythicland.mythiclogin.velocity.manager;

import com.velocitypowered.api.proxy.Player;

import java.util.List;
import java.util.Optional;

public interface IProxyRestrictionManager {

    Optional<String> findCaseMismatchKickMessage(String playerName);

    boolean shouldBlockCommand(Player player, String command);

    List<String> filterSuggestions(Player player, List<String> suggestions);

    void sendBlockedMessage(Player player);

    void refreshAuthentication(Player player);

    void clearAuthentication(Player player);

    void runStartupSanityCheck();
}
