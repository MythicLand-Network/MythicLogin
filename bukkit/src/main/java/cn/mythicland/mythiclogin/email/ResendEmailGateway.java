package cn.mythicland.mythiclogin.email;

import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.core.net.RequestOptions;
import com.resend.services.emails.model.CreateEmailOptions;

public final class ResendEmailGateway implements EmailGateway {
    private static final String BIND_SUBJECT = "【MythicLand Network】邮箱绑定验证码";
    private static final String RESET_SUBJECT = "【MythicLand Network】密码重置验证码";
    private static final String PASSWORD_CHANGED_SUBJECT = "【MythicLand Network】密码修改通知";
    private static final String EMAIL_CHANGED_SUBJECT = "【MythicLand Network】邮箱更换通知";

    private final ConfigManager configManager;
    private final ResendSender resendSender;

    public ResendEmailGateway(ConfigManager configManager) {
        this(configManager, (settings, options, requestOptions) ->
                new Resend(settings.getApiKey()).emails().send(options, requestOptions));
    }

    ResendEmailGateway(ConfigManager configManager, ResendSender resendSender) {
        this.configManager = configManager;
        this.resendSender = resendSender;
    }

    @Override
    public void send(VerificationEmail email) throws EmailDeliveryException {
        EmailSettings settings = configManager.getEmailSettings();
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(settings.getFrom())
                .to(email.recipient())
                .subject(subject(email.purpose()))
                .text(text(email))
                .html(html(email))
                .build();
        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey(email.idempotencyKey())
                .build();
        try {
            resendSender.send(settings, options, requestOptions);
        } catch (ResendException exception) {
            throw new EmailDeliveryException("Resend rejected the verification email", exception);
        }
    }

    @Override
    public void send(AccountNotificationEmail email) throws EmailDeliveryException {
        EmailSettings settings = configManager.getEmailSettings();
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(settings.getFrom())
                .to(email.recipient())
                .subject(notificationSubject(email.type()))
                .text(notificationText(email))
                .html(notificationHtml(email))
                .build();
        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey(email.idempotencyKey())
                .build();
        try {
            resendSender.send(settings, options, requestOptions);
        } catch (ResendException exception) {
            throw new EmailDeliveryException("Resend rejected the account security notification", exception);
        }
    }

    private String subject(VerificationPurpose purpose) {
        return purpose == VerificationPurpose.BIND_EMAIL ? BIND_SUBJECT : RESET_SUBJECT;
    }

    private String text(VerificationEmail email) {
        return "您好，玩家 " + email.playerName() + "：\n\n"
                + "您正在进行 MythicLand Network " + operationName(email.purpose()) + "操作。\n\n"
                + "验证码：" + email.code() + "\n"
                + "有效期：" + email.expireMinutes() + " 分钟\n\n"
                + "请勿将验证码告知他人。如非本人操作，请忽略此邮件。\n\n"
                + "此邮件由系统自动发送，请勿回复。";
    }

    private String html(VerificationEmail email) {
        return "<div style=\"max-width:560px;margin:0 auto;padding:32px;"
                + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
                + "color:#1f2937;line-height:1.7\">"
                + "<h2 style=\"margin:0 0 24px;color:#111827\">MythicLand Network</h2>"
                + "<p>您好，玩家 <strong>" + email.playerName() + "</strong>：</p>"
                + "<p>您正在进行 MythicLand Network " + operationName(email.purpose()) + "操作，"
                + "请使用以下验证码完成验证：</p>"
                + "<div style=\"margin:24px 0;padding:20px;text-align:center;background:#f3f4f6;"
                + "border-radius:8px;font-size:32px;font-weight:700;letter-spacing:8px;color:#111827\">"
                + email.code() + "</div>"
                + "<p>该验证码将在 <strong>" + email.expireMinutes() + " 分钟 </strong>后失效，"
                + "请勿将验证码告知他人。</p>"
                + "<p>如非本人操作，请忽略此邮件。</p>"
                + "<hr style=\"margin:28px 0;border:0;border-top:1px solid #e5e7eb\">"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px\">此邮件由系统自动发送，请勿回复。</p>"
                + "</div>";
    }

    private String operationName(VerificationPurpose purpose) {
        return purpose == VerificationPurpose.BIND_EMAIL ? "邮箱绑定" : "密码重置";
    }

    private String notificationSubject(AccountNotificationType type) {
        return type == AccountNotificationType.PASSWORD_CHANGED
                ? PASSWORD_CHANGED_SUBJECT
                : EMAIL_CHANGED_SUBJECT;
    }

    private String notificationText(AccountNotificationEmail email) {
        if (email.type() == AccountNotificationType.PASSWORD_CHANGED) {
            return "您好，玩家 " + email.playerName() + "：\n\n"
                    + "您的 MythicLand Network 账号密码已成功修改。\n\n"
                    + "如为本人操作，无需进行其他处理。如非本人操作，请尽快进入服务器使用 /fp 重置密码，"
                    + "并联系服务器管理员。\n\n"
                    + "此邮件由系统自动发送，请勿回复。";
        }
        return "您好，玩家 " + email.playerName() + "：\n\n"
                + "您的 MythicLand Network 账号已成功更换绑定邮箱，此邮箱将不再接收验证码及安全通知。\n\n"
                + "如非本人操作，请尽快联系服务器管理员。\n\n"
                + "此邮件由系统自动发送，请勿回复。";
    }

    private String notificationHtml(AccountNotificationEmail email) {
        String detail = email.type() == AccountNotificationType.PASSWORD_CHANGED
                ? "<p>您的 MythicLand Network 账号密码已成功修改。</p>"
                + "<p>如为本人操作，无需进行其他处理。如非本人操作，请尽快进入服务器使用 "
                + "<strong>/fp</strong> 重置密码，并联系服务器管理员。</p>"
                : "<p>您的 MythicLand Network 账号已成功更换绑定邮箱，"
                + "此邮箱将不再接收验证码及安全通知。</p>"
                + "<p>如非本人操作，请尽快联系服务器管理员。</p>";
        return "<div style=\"max-width:560px;margin:0 auto;padding:32px;"
                + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
                + "color:#1f2937;line-height:1.7\">"
                + "<h2 style=\"margin:0 0 24px;color:#111827\">MythicLand Network</h2>"
                + "<p>您好，玩家 <strong>" + email.playerName() + "</strong>：</p>"
                + detail
                + "<hr style=\"margin:28px 0;border:0;border-top:1px solid #e5e7eb\">"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px\">此邮件由系统自动发送，请勿回复。</p>"
                + "</div>";
    }

    @FunctionalInterface
    interface ResendSender {
        void send(
                EmailSettings settings,
                CreateEmailOptions options,
                RequestOptions requestOptions
        ) throws ResendException;
    }
}
