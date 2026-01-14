package com.cashbee.application.usecase.invoice;

import com.cashbee.application.dto.invoice.SendInvoiceEmailCommand;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.domain.model.User;
import com.cashbee.domain.model.UserNotificationPreference;
import com.cashbee.domain.repository.PaymentInvoiceRepository;
import com.cashbee.domain.repository.UserNotificationPreferenceRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Use case for sending payment invoice emails to users.
 *
 * Features:
 * - Send individual invoice emails
 * - Batch send unsent invoices
 * - Check user notification preferences
 * - Render Thymeleaf email template
 * - Track email send status
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SendInvoiceEmailUseCase {

    private final PaymentInvoiceRepository invoiceRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    private static final String EMAIL_TEMPLATE = "email/payment-invoice";
    private static final String EMAIL_SUBJECT_PREFIX = "[CashBee] Biên lai thanh toán ";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Send invoice email for a specific invoice.
     *
     * @param command Send email command
     * @return true if email was sent successfully
     */
    @Transactional
    public boolean execute(SendInvoiceEmailCommand command) {
        log.info("Sending invoice email: invoiceId={}", command.getInvoiceId());

        PaymentInvoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + command.getInvoiceId()));

        // Check if email already sent
        if (Boolean.TRUE.equals(invoice.getEmailSent())) {
            log.debug("Email already sent for invoice {}", command.getInvoiceId());
            return true;
        }

        // Check user preference
        if (!shouldSendEmail(invoice.getUserId())) {
            log.debug("User {} has disabled payment invoice emails", invoice.getUserId());
            return false;
        }

        try {
            // Get user info
            String email = command.getUserEmail();
            String userName = command.getUserName();

            if (email == null || email.isBlank()) {
                User user = userRepository.findById(invoice.getUserId())
                        .orElseThrow(() -> new NotFoundException("User not found: " + invoice.getUserId()));
                email = user.getEmail();
                userName = user.getFullName();
            }

            if (email == null || email.isBlank()) {
                log.warn("No email address for user {}", invoice.getUserId());
                invoiceRepository.markEmailFailed(invoice.getId(), "No email address");
                return false;
            }

            // Send email
            sendEmail(invoice, email, userName);

            // Mark as sent
            invoiceRepository.markEmailSent(invoice.getId(), LocalDateTime.now());
            log.info("Invoice email sent successfully: invoiceId={}, email={}", invoice.getId(), email);

            return true;

        } catch (Exception e) {
            log.error("Failed to send invoice email: invoiceId={}", command.getInvoiceId(), e);
            invoiceRepository.markEmailFailed(invoice.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Send emails for all unsent invoices (batch job).
     * Only sends to users who have enabled payment invoice emails.
     *
     * @return Number of emails sent successfully
     */
    @Transactional
    public int sendUnsentInvoices() {
        log.info("Starting batch send of unsent invoice emails");

        List<PaymentInvoice> unsentInvoices = invoiceRepository.findUnsentInvoices();
        if (unsentInvoices.isEmpty()) {
            log.debug("No unsent invoices found");
            return 0;
        }

        // Get user IDs that have email preference enabled
        List<Long> userIds = unsentInvoices.stream()
                .map(PaymentInvoice::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> enabledUserIds = preferenceRepository.findUserIdsWithEmailPaymentInvoiceEnabled(userIds);

        // Filter invoices to only include users with enabled preference
        List<PaymentInvoice> eligibleInvoices = unsentInvoices.stream()
                .filter(inv -> enabledUserIds.contains(inv.getUserId()))
                .collect(Collectors.toList());

        log.info("Found {} eligible invoices to send (out of {} unsent)",
                eligibleInvoices.size(), unsentInvoices.size());

        // Get user info map
        Map<Long, User> userMap = userRepository.findAllById(enabledUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        int successCount = 0;
        for (PaymentInvoice invoice : eligibleInvoices) {
            try {
                User user = userMap.get(invoice.getUserId());
                if (user != null && user.getEmail() != null) {
                    sendEmail(invoice, user.getEmail(), user.getFullName());
                    invoiceRepository.markEmailSent(invoice.getId(), LocalDateTime.now());
                    successCount++;
                } else {
                    invoiceRepository.markEmailFailed(invoice.getId(), "No email address");
                }
            } catch (Exception e) {
                log.error("Failed to send email for invoice {}", invoice.getId(), e);
                invoiceRepository.markEmailFailed(invoice.getId(), e.getMessage());
            }
        }

        log.info("Batch email send completed: {}/{} successful", successCount, eligibleInvoices.size());
        return successCount;
    }

    /**
     * Async method to send invoice email in background.
     */
    @Async
    public void sendAsync(SendInvoiceEmailCommand command) {
        execute(command);
    }

    /**
     * Check if user has enabled payment invoice emails.
     */
    private boolean shouldSendEmail(Long userId) {
        Optional<UserNotificationPreference> preference = preferenceRepository.findByUserId(userId);

        // Default to true if no preference set
        if (preference.isEmpty()) {
            return true;
        }

        Boolean emailEnabled = preference.get().getEmailPaymentInvoice();
        return emailEnabled == null || emailEnabled;
    }

    /**
     * Render and send email using Thymeleaf template.
     */
    private void sendEmail(PaymentInvoice invoice, String toEmail, String userName) throws MessagingException {
        // Prepare template context
        Context context = new Context();
        context.setVariable("userName", userName != null ? userName : "Quý khách");
        context.setVariable("invoiceNumber", invoice.getInvoiceNumber());
        context.setVariable("amount", invoice.getAmount());
        context.setVariable("currency", invoice.getCurrency());
        context.setVariable("bankName", invoice.getBankName());
        context.setVariable("bankAccountNumber", maskAccountNumber(invoice.getAccountNumber()));
        context.setVariable("transferTime", formatDateTime(invoice.getTransferTime()));
        context.setVariable("totalOrders", invoice.getTotalOrders());

        // Platform breakdown
        context.setVariable("shopeeOrders", invoice.getShopeeOrders());
        context.setVariable("shopeeAmount", invoice.getShopeeAmount());
        context.setVariable("lazadaOrders", invoice.getLazadaOrders());
        context.setVariable("lazadaAmount", invoice.getLazadaAmount());
        context.setVariable("tikiOrders", invoice.getTikiOrders());
        context.setVariable("tikiAmount", invoice.getTikiAmount());
        context.setVariable("tiktokOrders", invoice.getTiktokOrders());
        context.setVariable("tiktokAmount", invoice.getTiktokAmount());
        context.setVariable("otherOrders", invoice.getOtherOrders());
        context.setVariable("otherAmount", invoice.getOtherAmount());

        // Render template
        String htmlContent = templateEngine.process(EMAIL_TEMPLATE, context);

        // Create email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject(EMAIL_SUBJECT_PREFIX + invoice.getInvoiceNumber());
        helper.setText(htmlContent, true);

        // Send
        mailSender.send(message);
    }

    /**
     * Mask bank account number for display (show only last 4 digits).
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        int length = accountNumber.length();
        return "*".repeat(length - 4) + accountNumber.substring(length - 4);
    }

    /**
     * Format datetime for display.
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
}
