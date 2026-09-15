package cn.mythicland.mythiclogin.email;

public final class EmailDeliveryException extends Exception {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
