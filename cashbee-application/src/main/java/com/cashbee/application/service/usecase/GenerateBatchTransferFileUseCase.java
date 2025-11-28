package com.cashbee.application.service.usecase;

import com.cashbee.application.dto.batch.BatchTransferRow;
import com.cashbee.application.service.excel.BatchTransferExcelGenerator;
import com.cashbee.application.service.excel.ExcelGeneratorFactory;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.BankTemplate;
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
 * 4. Uses ExcelGeneratorFactory to select appropriate generator (VPBank or VietinBank)
 * 5. Returns byte array
 *
 * Supports multiple bank templates:
 * - VPBANK: .xls format with bank_name
 * - VIETINBANK: .xlsx format with vietinbank_code (8 digits)
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
    private final ExcelGeneratorFactory excelGeneratorFactory;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Generate Excel file by batch code with default template (VPBANK).
     *
     * @param batchCode Batch code
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateByBatchCode(String batchCode) {
        return generateByBatchCode(batchCode, BankTemplate.VPBANK);
    }

    /**
     * Generate Excel file by batch code with specified bank template.
     *
     * @param batchCode Batch code
     * @param bankTemplate Bank template type (VPBANK or VIETINBANK)
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateByBatchCode(String batchCode, BankTemplate bankTemplate) {
        log.info("GenerateBatchTransferFileUseCase: Generating file for batch code: {}, template: {}",
                batchCode, bankTemplate);

        // Load batch metadata
        BatchTransferExport batchExport = batchTransferExportRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new BusinessException("BATCH_NOT_FOUND",
                        "Batch transfer export not found: " + batchCode));

        // Generate file with stored minBalance and remark template
        return generateFile(batchExport.getMinBalance(), batchExport.getRemarkTemplate(), bankTemplate);
    }

    /**
     * Generate Excel file with custom criteria and default template (VPBANK).
     *
     * @param minBalance Minimum balance
     * @param remarkTemplate Remark template
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateFile(BigDecimal minBalance, String remarkTemplate) {
        return generateFile(minBalance, remarkTemplate, BankTemplate.VPBANK);
    }

    /**
     * Generate Excel file with custom criteria and specified bank template.
     *
     * @param minBalance Minimum balance
     * @param remarkTemplate Remark template
     * @param bankTemplate Bank template type (VPBANK or VIETINBANK)
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateFile(BigDecimal minBalance, String remarkTemplate, BankTemplate bankTemplate) {
        log.info("GenerateBatchTransferFileUseCase: Generating file with minBalance={}, template={}",
                minBalance, bankTemplate);

        // 1. Generate remark if not provided
        String remark = remarkTemplate;
        if (remark == null || remark.isBlank()) {
            remark = generateDefaultRemark();
        }

        // 2. Query eligible users (with vietinbank_code for VietinBank template)
        List<BatchTransferRow> rows = queryEligibleUsersWithBankAccounts(minBalance, remark);

        if (rows.isEmpty()) {
            throw new BusinessException("NO_ELIGIBLE_USERS",
                    "No users found with balance >= " + minBalance + " VND and valid bank account");
        }

        log.info("GenerateBatchTransferFileUseCase: Found {} eligible users", rows.size());

        // 3. Get appropriate generator from factory
        BatchTransferExcelGenerator generator = excelGeneratorFactory.getGenerator(bankTemplate);

        // 4. Generate Excel file
        byte[] excelBytes = generator.generate(rows);

        log.info("GenerateBatchTransferFileUseCase: Generated {} file ({} bytes)",
                bankTemplate.getFileExtension(), excelBytes.length);

        return excelBytes;
    }

    /**
     * Query eligible users with bank account details.
     *
     * Returns realtime data from database.
     * Includes vietinbank_code from bank table for VietinBank template support.
     */
    private List<BatchTransferRow> queryEligibleUsersWithBankAccounts(BigDecimal minBalance, String remarkTemplate) {
        String sql = """
                SELECT
                    u.id as user_id,
                    uw.balance,
                    uba.account_number,
                    uba.account_name,
                    uba.bank_name,
                    uba.bank_code,
                    b.vietinbank_code,
                    COALESCE(b.vpbank_id, b.id) as vpbank_id
                FROM user u
                INNER JOIN user_wallet uw ON u.id = uw.user_id
                INNER JOIN user_bank_account uba ON u.id = uba.user_id
                LEFT JOIN bank b ON uba.bank_code = b.bank_code
                WHERE uw.balance >= ?
                AND u.deleted_at IS NULL
                AND uba.is_default = 1
                ORDER BY uw.balance DESC, u.id ASC
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
     * Maps database result to DTO including vietinbank_code.
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
                    .bankCode(rs.getString("bank_code"))
                    .vietinbankCode(rs.getString("vietinbank_code"))
                    .vpbankId(rs.getInt("vpbank_id"))
                    .remark(remarkTemplate)
                    .build();
        }
    }
}
