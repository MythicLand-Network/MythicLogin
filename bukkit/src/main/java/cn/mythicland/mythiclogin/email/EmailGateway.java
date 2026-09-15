package cn.mythicland.mythiclogin.email;

public interface EmailGateway {
    void send(VerificationEmail email) throws EmailDeliveryException;

    void send(AccountNotificationEmail email) throws EmailDeliveryException;
}
