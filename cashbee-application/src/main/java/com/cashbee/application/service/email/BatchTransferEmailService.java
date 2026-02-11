package com.cashbee.application.service.email;

import com.cashbee.common.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service to send batch transfer Excel files via email.
 *
 * Sends file as attachment to admin email address.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchTransferEmailService {

    private final JavaMailSender mailSender;

    @Value("${cashbee.batch-transfer.admin-email:admin@cashbee.com}")
    private String adminEmail;

    @Value("${spring.mail.username:noreply@cashbee.com}")
    private String fromEmail;

    /**
     * Send batch transfer file via email.
     *
     * @param fileName File name (e.g., BATCH_20251119_001.xls)
     * @param fileBytes Excel file content
     * @param totalUsers Total users in batch
     * @param totalAmount Total amount (VND)
     * @param recipientEmail Email address to send to (nullable - defaults to admin)
     */
    public void sendBatchTransferFile(
            String fileName,
            byte[] fileBytes,
            Integer totalUsers,
            BigDecimal totalAmount,
            String recipientEmail) {

        log.info("BatchTransferEmailService: Sending batch transfer file {} to {}",
                fileName, recipientEmail != null ? recipientEmail : adminEmail);

        try {
            // Create MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set sender and recipient
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail != null ? recipientEmail : adminEmail);

            // Set subject
            String subject = "CashBee Batch Transfer - " + fileName;
            helper.setSubject(subject);

            // Set email body
            String body = buildEmailBody(fileName, totalUsers, totalAmount);
            helper.setText(body, true); // true = HTML

            // Attach Excel file
            ByteArrayResource attachment = new ByteArrayResource(fileBytes);
            helper.addAttachment(fileName, attachment);

            // Send email
            mailSender.send(message);

            log.info("BatchTransferEmailService: Email sent successfully to {}",
                    recipientEmail != null ? recipientEmail : adminEmail);

        } catch (MessagingException e) {
            log.error("BatchTransferEmailService: Failed to send email", e);
            throw new BusinessException("EMAIL_SEND_FAILED",
                    "Failed to send batch transfer email: " + e.getMessage());
        }
    }

    /**
     * Build HTML email body.
     */
    private String buildEmailBody(String fileName, Integer totalUsers, BigDecimal totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String formattedAmount = String.format("%,d", totalAmount.longValue());

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .header {
                            background-color: #4CAF50;
                            color: white;
                            padding: 20px;
                            text-align: center;
                            border-radius: 5px 5px 0 0;
                        }
                        .content {
                            background-color: #f9f9f9;
                            padding: 20px;
                            border: 1px solid #ddd;
                            border-top: none;
                            border-radius: 0 0 5px 5px;
                        }
                        .info-row {
                            margin: 10px 0;
                            padding: 10px;
                            background-color: white;
                            border-left: 4px solid #4CAF50;
                        }
                        .label {
                            font-weight: bold;
                            color: #555;
                        }
                        .value {
                            color: #333;
                        }
                        .footer {
                            margin-top: 20px;
                            padding-top: 20px;
                            border-top: 1px solid #ddd;
                            font-size: 12px;
                            color: #777;
                            text-align: center;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>CashBee - Batch Transfer Export</h2>
                        </div>
                        <div class="content">
                            <p>Xin chào,</p>
                            <p>File batch transfer đã được tạo thành công. Vui lòng xem chi tiết bên dưới:</p>

                            <div class="info-row">
                                <span class="label">Tên file:</span>
                                <span class="value">%s</span>
                            </div>

                            <div class="info-row">
                                <span class="label">Số lượng user:</span>
                                <span class="value">%d người</span>
                            </div>

                            <div class="info-row">
                                <span class="label">Tổng số tiền:</span>
                                <span class="value">%s VND</span>
                            </div>

                            <div class="info-row">
                                <span class="label">Thời gian tạo:</span>
                                <span class="value">%s</span>
                            </div>

                            <p style="margin-top: 20px;">
                                <strong>Lưu ý:</strong> File Excel đính kèm có thể được upload trực tiếp lên hệ thống VPBank để thực hiện chuyển khoản batch.
                            </p>

                            <p>Cảm ơn bạn đã sử dụng CashBee!</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 CashBee. All rights reserved.</p>
                            <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(fileName, totalUsers, formattedAmount, formattedDateTime);
    }
}
