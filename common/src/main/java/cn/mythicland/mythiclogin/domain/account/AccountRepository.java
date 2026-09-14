package cn.mythicland.mythiclogin.domain.account;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findByExactPlayerName(String playerName);

    Optional<Account> findByPlayerNameIgnoreCase(String playerName);

    boolean create(Account account);

    boolean updatePassword(String username, String passwordHash);

    boolean updateLoginMeta(String username, String ip, long lastLogin, boolean hasSession);

    boolean updateLoggedState(String username, boolean logged);

    boolean isLogged(String username);

    boolean deleteByExactPlayerName(String playerName);

    int countAccounts();
}
