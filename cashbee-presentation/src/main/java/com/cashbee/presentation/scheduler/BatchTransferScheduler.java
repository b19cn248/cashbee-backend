package com.cashbee.presentation.scheduler;

import com.cashbee.application.dto.batch.ExportBatchTransferRequest;
import com.cashbee.application.dto.batch.ExportBatchTransferResponse;
import com.cashbee.application.service.email.BatchTransferEmailService;
import com.cashbee.application.service.usecase.ExportBatchTransferUseCase;
import com.cashbee.application.service.usecase.GenerateBatchTransferFileUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Scheduled job for automatic batch transfer exports.
 *
 * Runs every Monday at 08:00 (configurable via cron expression).
 *
 * What it does:
 * 1. Creates batch export metadata
 * 2. Generates Excel file
 * 3. Sends file via email to admin
 *
 * Can be disabled via application.yml:
 * cashbee.batch-transfer.scheduler.enabled: false
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "cashbee.batch-transfer.scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BatchTransferScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchTransferScheduler.class);

    private final ExportBatchTransferUseCase exportBatchTransferUseCase;
    private final GenerateBatchTransferFileUseCase generateBatchTransferFileUseCase;
    private final BatchTransferEmailService emailService;

    @Value("${cashbee.batch-transfer.admin-email:admin@cashbee.com}")
    private String adminEmail;

    @Value("${cashbee.batch-transfer.min-balance:50000}")
    private BigDecimal minBalance;

    /**
     * Scheduled task: Export batch transfer file every Tuesday at 08:00.
     *
     * Cron expression: "0 0 8 * * TUE"
     * - Second: 0
     * - Minute: 0
     * - Hour: 8
     * - Day of month: * (any)
     * - Month: * (any)
     * - Day of week: TUE (Tuesday)
     */
    @Scheduled(cron = "${cashbee.batch-transfer.scheduler.cron:0 0 8 * * TUE}")
    public void exportAndSendBatchTransferFile() {
        log.info("BatchTransferScheduler: Starting scheduled batch transfer export");

        try {
            // 1. Create batch export metadata
            ExportBatchTransferRequest request = ExportBatchTransferRequest.builder()
                    .minBalance(minBalance)
                    .exportType("SCHEDULED")
                    .build();

            ExportBatchTransferResponse response = exportBatchTransferUseCase.execute(request);

            log.info("BatchTransferScheduler: Batch export created - {}", response.getBatchCode());

            // 2. Generate Excel file
            byte[] excelBytes = generateBatchTransferFileUseCase.generateByBatchCode(response.getBatchCode());

            log.info("BatchTransferScheduler: Excel file generated ({} bytes)", excelBytes.length);

            // 3. Send via email
            emailService.sendBatchTransferFile(
                    response.getFileName(),
                    excelBytes,
                    response.getTotalUsers(),
                    response.getTotalAmount(),
                    adminEmail
            );

            log.info("BatchTransferScheduler: Batch transfer file sent successfully to {}", adminEmail);

        } catch (Exception e) {
            log.error("BatchTransferScheduler: Failed to export and send batch transfer file", e);
            // Don't rethrow - let scheduler continue running
        }
    }
}
