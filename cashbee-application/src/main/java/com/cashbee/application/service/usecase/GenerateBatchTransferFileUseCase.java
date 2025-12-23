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
     * FIX: Giờ đây lấy data từ batch_transfer_item table (snapshot data)
     * thay vì query lại tất cả users với balance >= minBalance.
     *
     * @param batchCode Batch code
     * @param bankTemplate Bank template type (VPBANK or VIETINBANK)
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateByBatchCode(String batchCode, BankTemplate bankTemplate) {
        log.info("GenerateBatchTransferFileUseCase: Generating file for batch code: {}, template: {}",
                batchCode, bankTemplate);

        // 1. Load batch metadata
        BatchTransferExport batchExport = batchTransferExportRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new BusinessException("BATCH_NOT_FOUND",
                        "Batch transfer export not found: " + batchCode));

        // 2. Query batch items (snapshot data từ khi tạo batch)
        List<BatchTransferRow> rows = queryBatchItemsWithBankInfo(
                batchExport.getId(),
                batchExport.getRemarkTemplate()
        );

        if (rows.isEmpty()) {
            throw new BusinessException("NO_BATCH_ITEMS",
                    "No items found for batch: " + batchCode);
        }

        log.info("GenerateBatchTransferFileUseCase: Found {} items in batch {}", rows.size(), batchCode);

        // 3. Get appropriate generator from factory
        BatchTransferExcelGenerator generator = excelGeneratorFactory.getGenerator(bankTemplate);

        // 4. Generate Excel file
        byte[] excelBytes = generator.generate(rows);

        log.info("GenerateBatchTransferFileUseCase: Generated {} file ({} bytes) for batch {}",
                bankTemplate.getFileExtension(), excelBytes.length, batchCode);

        return excelBytes;
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
     * Includes vpbank_code (9-digit) and vietinbank_code from bank table.
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
                    b.vpbank_code,
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
     * Query batch items với bank info từ batch_transfer_item table.
     *
     * Đây là data đã được snapshot khi tạo batch, đảm bảo:
     * - Chỉ lấy đúng những users thuộc batch này
     * - Amount là giá trị tại thời điểm tạo batch (không phải balance hiện tại)
     *
     * @param batchId Batch ID
     * @param remarkTemplate Remark template
     * @return List of BatchTransferRow
     */
    private List<BatchTransferRow> queryBatchItemsWithBankInfo(Long batchId, String remarkTemplate) {
        String sql = """
                SELECT
                    bti.id,
                    bti.user_id,
                    bti.amount,
                    bti.account_number,
                    bti.account_name,
                    bti.bank_name,
                    uba.bank_code,
                    b.vpbank_code,
                    b.vietinbank_code,
                    COALESCE(b.vpbank_id, b.id) as vpbank_id
                FROM batch_transfer_item bti
                LEFT JOIN user_bank_account uba ON bti.user_id = uba.user_id AND uba.is_default = 1
                LEFT JOIN bank b ON uba.bank_code = b.bank_code
                WHERE bti.batch_id = ?
                ORDER BY bti.id ASC
                """;

        return jdbcTemplate.query(sql, new BatchItemRowMapper(remarkTemplate), batchId);
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
     * Row mapper for BatchTransferRow (used for fresh query with minBalance).
     * Maps database result to DTO including vpbank_code and vietinbank_code.
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
                    .currency("VND")  // Default currency for CashBee
                    .bankName(rs.getString("bank_name"))
                    .bankCode(rs.getString("bank_code"))
                    .vpbankCode(rs.getString("vpbank_code"))
                    .vietinbankCode(rs.getString("vietinbank_code"))
                    .vpbankId(rs.getInt("vpbank_id"))
                    .remark(remarkTemplate)
                    .charges("OUR")  // Default: sender pays fees
                    .build();
        }
    }

    /**
     * Row mapper for batch items (used for query from batch_transfer_item table).
     * Maps snapshot data from batch_transfer_item with bank info.
     */
    private static class BatchItemRowMapper implements RowMapper<BatchTransferRow> {

        private final String remarkTemplate;
        private int stt = 1;

        public BatchItemRowMapper(String remarkTemplate) {
            this.remarkTemplate = remarkTemplate;
        }

        @Override
        public BatchTransferRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return BatchTransferRow.builder()
                    .stt(stt++)
                    .userId(rs.getLong("user_id"))
                    .accountNumber(rs.getString("account_number"))
                    .accountName(rs.getString("account_name"))
                    .amount(rs.getBigDecimal("amount"))  // từ batch_transfer_item, không phải balance
                    .currency("VND")  // Default currency for CashBee
                    .bankName(rs.getString("bank_name"))
                    .bankCode(rs.getString("bank_code"))
                    .vpbankCode(rs.getString("vpbank_code"))
                    .vietinbankCode(rs.getString("vietinbank_code"))
                    .vpbankId(rs.getInt("vpbank_id"))
                    .remark(remarkTemplate)
                    .charges("OUR")  // Default: sender pays fees
                    .build();
        }
    }
}
