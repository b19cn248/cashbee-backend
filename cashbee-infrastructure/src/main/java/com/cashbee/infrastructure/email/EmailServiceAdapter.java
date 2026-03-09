package com.cashbee.infrastructure.email;

import com.cashbee.domain.port.EmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * Email service adapter using Spring JavaMailSender and Thymeleaf templates.
 * Implements the EmailPort interface from the domain layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceAdapter implements EmailPort {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${cashbee.app.name:CashBee}")
    private String appName;

    @Value("${cashbee.app.url:https://cashbee.nguocchieuvangle.io.vn}")
    private String appUrl;

    @Override
    public void sendOtpEmail(String to, String otpCode, int expiryMinutes) {
        log.info("Sending OTP email to: {}", to);

        try {
            // Prepare template context
            Context context = new Context();
            context.setVariable("otpCode", otpCode);
            context.setVariable("expiryMinutes", expiryMinutes);
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);

            // Process template
            String htmlContent = templateEngine.process("emails/otp-email", context);

            // Send email
            sendHtmlEmail(
                to,
                appName + " - Mã xác thực OTP của bạn",
                htmlContent
            );

            log.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", to, e);
            throw new EmailSendException("Failed to send OTP email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String to, String username, String fullName, String referralCode) {
        log.info("Sending welcome email to: {}", to);

        try {
            // Prepare template context
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("fullName", fullName != null ? fullName : username);
            context.setVariable("referralCode", referralCode);
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("referralUrl", appUrl + "?ref=" + referralCode);

            // Process template
            String htmlContent = templateEngine.process("emails/welcome-email", context);

            // Send email
            sendHtmlEmail(
                to,
                "Chào mừng bạn đến với " + appName + "! 🎉",
                htmlContent
            );

            log.info("Welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
            // Don't throw exception for welcome email - it's not critical
            // User is already registered, just log the error
            log.warn("Continuing despite welcome email failure");
        }
    }

    @Override
    public void sendPasswordResetOtpEmail(String to, String otpCode, int expiryMinutes) {
        log.info("Sending password reset OTP email to: {}", to);

        try {
            Context context = new Context();
            context.setVariable("otpCode", otpCode);
            context.setVariable("expiryMinutes", expiryMinutes);
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);

            String htmlContent = templateEngine.process("emails/password-reset-otp-email", context);

            sendHtmlEmail(
                to,
                appName + " - Mã xác thực đặt lại mật khẩu",
                htmlContent
            );

            log.info("Password reset OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to: {}", to, e);
            throw new EmailSendException("Failed to send password reset OTP email", e);
        }
    }

    /**
     * Send HTML email using JavaMailSender
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
            message,
            MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
            StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(message);
    }
}
