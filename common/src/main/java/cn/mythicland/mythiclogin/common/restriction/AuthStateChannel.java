package cn.mythicland.mythiclogin.common.restriction;

public final class AuthStateChannel {
    public static final String CHANNEL = "mythiclogin:state";
    public static final byte AUTHENTICATED = 1;
    public static final byte UNAUTHENTICATED = 0;
    public static final byte QUERY = 2;

    private AuthStateChannel() {}
}
