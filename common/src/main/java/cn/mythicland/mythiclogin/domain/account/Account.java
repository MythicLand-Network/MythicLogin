package cn.mythicland.mythiclogin.domain.account;

public final class Account {

    private final String username;
    private final String realName;
    private final String password;
    private final String ip;
    private final long lastLogin;
    private final long registerDate;
    private final String registerIp;
    private final String email;
    private final boolean hasSession;
    private final boolean logged;

    public Account(String username, String realName, String password, String ip, long lastLogin, long registerDate, String registerIp, boolean hasSession, boolean logged) {
        this(username, realName, password, ip, lastLogin, registerDate, registerIp, null, hasSession, logged);
    }

    public Account(String username, String realName, String password, String ip, long lastLogin, long registerDate, String registerIp, String email, boolean hasSession, boolean logged) {
        this.username = username;
        this.realName = realName;
        this.password = password;
        this.ip = ip;
        this.lastLogin = lastLogin;
        this.registerDate = registerDate;
        this.registerIp = registerIp;
        this.email = email;
        this.hasSession = hasSession;
        this.logged = logged;
    }

    public String getUsername() {
        return username;
    }

    public String getRealName() {
        return realName;
    }

    public String getPassword() {
        return password;
    }

    public String getIp() {
        return ip;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public long getRegisterDate() {
        return registerDate;
    }

    public String getRegisterIp() {
        return registerIp;
    }

    public String getEmail() {
        return email;
    }

    public boolean isHasSession() {
        return hasSession;
    }

    public boolean isLogged() {
        return logged;
    }

    public Account withPassword(String newPassword) {
        return new Account(username, realName, newPassword, ip, lastLogin, registerDate, registerIp, email, hasSession, logged);
    }

    public Account withLoginMeta(String newIp, long newLastLogin, boolean session) {
        return new Account(username, realName, password, newIp, newLastLogin, registerDate, registerIp, email, session, logged);
    }

    public Account withLoggedState(boolean newLoggedState) {
        return new Account(username, realName, password, ip, lastLogin, registerDate, registerIp, email, hasSession, newLoggedState);
    }

    public Account withEmail(String newEmail) {
        return new Account(username, realName, password, ip, lastLogin, registerDate, registerIp, newEmail, hasSession, logged);
    }
}
