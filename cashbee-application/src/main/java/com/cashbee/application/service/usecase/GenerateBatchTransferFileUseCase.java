package com.cashbee.application.service.usecase;

import com.cashbee.application.dto.batch.BatchTransferRow;
import com.cashbee.application.service.excel.VPBankExcelGenerator;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.domain.repository.BatchTransferExportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Use case for generating batch transfer Excel file.
 *
 * This use case:
 * 1. Loads batch metadata (optional - can generate fresh)
 * 2. Queries eligible users with bank accounts (realtime data)
 * 3. Builds list of BatchTransferRow DTOs
 * 4. Calls VPBankExcelGenerator to create Excel file
 * 5. Returns byte array
 *
 * NOTE: This always generates fresh data (realtime).
 * Does NOT load cached/stored files.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateBatchTransferFileUseCase {

    private final BatchTransferExportRepository batchTransferExportRepository;
    private final VPBankExcelGenerator excelGenerator;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Generate Excel file by batch code.
     *
     * @param batchCode Batch code
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateByBatchCode(String batchCode) {
        log.info("GenerateBatchTransferFileUseCase: Generating file for batch code: {}", batchCode);

        // Load batch metadata
        BatchTransferExport batchExport = batchTransferExportRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new BusinessException("BATCH_NOT_FOUND",
                        "Batch transfer export not found: " + batchCode));

        // Generate file with stored remark template
        return generateFile(new BigDecimal("50000"), batchExport.getRemarkTemplate());
    }

    /**
     * Generate Excel file with custom criteria.
     *
     * @param minBalance Minimum balance
     * @param remarkTemplate Remark template
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateFile(BigDecimal minBalance, String remarkTemplate) {
        log.info("GenerateBatchTransferFileUseCase: Generating file with minBalance={}", minBalance);

        // 1. Generate remark if not provided
        String remark = remarkTemplate;
        if (remark == null || remark.isBlank()) {
            remark = generateDefaultRemark();
        }

        // 2. Query eligible users
        List<BatchTransferRow> rows = queryEligibleUsersWithBankAccounts(minBalance, remark);

        if (rows.isEmpty()) {
            throw new BusinessException("NO_ELIGIBLE_USERS",
                    "No users found with balance >= " + minBalance + " VND and valid bank account");
        }

        log.info("GenerateBatchTransferFileUseCase: Found {} eligible users", rows.size());

        // 3. Generate Excel file
        byte[] excelBytes = excelGenerator.generate(rows);

        log.info("GenerateBatchTransferFileUseCase: Generated Excel file ({} bytes)", excelBytes.length);

        return excelBytes;
    }

    /**
     * Query eligible users with bank account details.
     *
     * Returns realtime data from database.
     */
    private List<BatchTransferRow> queryEligibleUsersWithBankAccounts(BigDecimal minBalance, String remarkTemplate) {
        String sql = """
                SELECT
                    u.id as user_id,
                    u.balance,
                    uba.account_number,
                    uba.account_name,
                    uba.bank_name
                FROM users u
                INNER JOIN user_bank_account uba ON u.id = uba.user_id
                WHERE u.balance >= ?
                AND u.deleted_at IS NULL
                AND uba.deleted_at IS NULL
                ORDER BY u.balance DESC, u.id ASC
                """;

        return jdbcTemplate.query(sql, new BatchTransferRowMapper(remarkTemplate), minBalance);
    }

    /**
     * Generate default remark template.
     */
    private String generateDefaultRemark() {
        LocalDateTime now = LocalDateTime.now();
        String monthYear = now.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        return "Hoan tien CashBee " + monthYear;
    }

    /**
     * Row mapper for BatchTransferRow.
     */
    private static class BatchTransferRowMapper implements RowMapper<BatchTransferRow> {

        private final String remarkTemplate;
        private int stt = 1;

        public BatchTransferRowMapper(String remarkTemplate) {
            this.remarkTemplate = remarkTemplate;
        }

        @Override
        public BatchTransferRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return BatchTransferRow.builder()
                    .stt(stt++)
                    .userId(rs.getLong("user_id"))
                    .accountNumber(rs.getString("account_number"))
                    .accountName(rs.getString("account_name"))
                    .amount(rs.getBigDecimal("balance"))
                    .bankName(rs.getString("bank_name"))
                    .remark(remarkTemplate)
                    .build();
        }
    }
}
