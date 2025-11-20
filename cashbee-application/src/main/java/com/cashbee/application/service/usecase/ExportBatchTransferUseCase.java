package com.cashbee.application.service.usecase;

import com.cashbee.application.dto.batch.ExportBatchTransferRequest;
import com.cashbee.application.dto.batch.ExportBatchTransferResponse;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.ExportStatus;
import com.cashbee.domain.enums.ExportType;
import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.domain.repository.BatchTransferExportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Use case for creating batch transfer export metadata.
 *
 * This use case:
 * 1. Queries eligible users (balance >= minBalance, has bank account)
 * 2. Calculates totals
 * 3. Generates batch code
 * 4. Saves metadata to database
 * 5. Returns response DTO
 *
 * NOTE: This does NOT generate the actual Excel file.
 * File generation is done by GenerateBatchTransferFileUseCase.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportBatchTransferUseCase {

    private final BatchTransferExportRepository batchTransferExportRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Execute export batch transfer.
     *
     * @param request Export request
     * @return Export response with metadata
     */
    @Transactional
    public ExportBatchTransferResponse execute(ExportBatchTransferRequest request) {
        log.info("ExportBatchTransferUseCase: Starting export with minBalance={}", request.getMinBalance());

        // 1. Query eligible users
        EligibleUsersResult result = queryEligibleUsers(request.getMinBalance());

        log.info("ExportBatchTransferUseCase: Found {} eligible users with total amount: {}",
                result.getTotalUsers(), result.getTotalAmount());

        if (result.getTotalUsers() == 0) {
            throw new BusinessException("NO_ELIGIBLE_USERS",
                    "No users found with balance >= " + request.getMinBalance() + " VND and valid bank account");
        }

        // 2. Generate batch code
        String batchCode = generateBatchCode();

        // 3. Generate remark if not provided
        String remark = request.getRemarkTemplate();
        if (remark == null || remark.isBlank()) {
            remark = generateDefaultRemark();
        }

        // 4. Determine export type
        ExportType exportType = ExportType.MANUAL;
        if (request.getExportType() != null && request.getExportType().equalsIgnoreCase("SCHEDULED")) {
            exportType = ExportType.SCHEDULED;
        }

        // 5. Create domain model
        BatchTransferExport batchExport = BatchTransferExport.builder()
                .batchCode(batchCode)
                .fileName(batchCode + ".xls")
                .totalUsers(result.getTotalUsers())
                .totalAmount(result.getTotalAmount())
                .status(ExportStatus.COMPLETED)
                .exportType(exportType)
                .remarkTemplate(remark)
                .createdAt(LocalDateTime.now())
                .build();

        // 6. Validate and save
        batchExport.validate();
        BatchTransferExport saved = batchTransferExportRepository.save(batchExport);

        log.info("ExportBatchTransferUseCase: Saved batch export with code: {}", saved.getBatchCode());

        // 7. Build response
        return ExportBatchTransferResponse.builder()
                .batchCode(saved.getBatchCode())
                .fileName(saved.getFileName())
                .totalUsers(saved.getTotalUsers())
                .totalAmount(saved.getTotalAmount())
                .exportedAt(saved.getCreatedAt())
                .message(String.format(
                        "Export completed. %d users eligible for transfer (total: %,d VND)",
                        saved.getTotalUsers(),
                        saved.getTotalAmount().longValue()
                ))
                .build();
    }

    /**
     * Query eligible users from database.
     *
     * Users are eligible if:
     * - balance >= minBalance
     * - has bank account configured
     */
    private EligibleUsersResult queryEligibleUsers(BigDecimal minBalance) {
        String sql = """
                SELECT COUNT(*) as total_users, COALESCE(SUM(u.balance), 0) as total_amount
                FROM users u
                INNER JOIN user_bank_account uba ON u.id = uba.user_id
                WHERE u.balance >= ?
                AND u.deleted_at IS NULL
                AND uba.deleted_at IS NULL
                """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, minBalance);

        Integer totalUsers = ((Number) result.get("total_users")).intValue();
        BigDecimal totalAmount = (BigDecimal) result.get("total_amount");

        return new EligibleUsersResult(totalUsers, totalAmount);
    }

    /**
     * Generate unique batch code.
     * Format: BATCH_YYYYMMDD_XXX
     * Example: BATCH_20251119_001
     */
    private String generateBatchCode() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Get count of batches created today
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        long countToday = batchTransferExportRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        // Increment for next batch
        String sequence = String.format("%03d", countToday + 1);

        return "BATCH_" + dateStr + "_" + sequence;
    }

    /**
     * Generate default remark template.
     * Format: "Hoan tien CashBee MM/YYYY"
     * Example: "Hoan tien CashBee 11/2025"
     */
    private String generateDefaultRemark() {
        LocalDate today = LocalDate.now();
        String monthYear = today.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        return "Hoan tien CashBee " + monthYear;
    }

    /**
     * Result DTO for eligible users query.
     */
    private record EligibleUsersResult(Integer totalUsers, BigDecimal totalAmount) {
        public Integer getTotalUsers() {
            return totalUsers;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }
}
