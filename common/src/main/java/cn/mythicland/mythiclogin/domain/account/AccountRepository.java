package cn.mythicland.mythiclogin.domain.account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findByExactPlayerName(String playerName);

    Optional<Account> findByPlayerNameIgnoreCase(String playerName);

    Optional<Account> findByEmailIgnoreCase(String email);

    boolean create(Account account);

    boolean bindPremium(Account expected, UUID premiumUuid);

    boolean unbindPremium(Account expected, String passwordHash);

    boolean updatePassword(String username, String passwordHash);

    boolean resetPassword(String username, String passwordHash);

    boolean updateEmailIfAvailable(String username, String expectedEmail, String email);

    boolean updateLoginMeta(String username, String ip, long lastLogin, boolean hasSession);

    boolean updateLoggedState(String username, boolean logged);

    boolean isLogged(String username);

    boolean deleteByExactPlayerName(String playerName);

    int countAccounts();
}
