package com.cashbee.application.service.excel;

import com.cashbee.application.dto.batch.BatchTransferRow;
import com.cashbee.domain.enums.BankTemplate;

import java.util.List;

/**
 * Interface for generating batch transfer Excel files.
 *
 * Different banks require different file formats:
 * - VPBank: .xls format with specific columns
 * - VietinBank: .xlsx format with different column requirements
 *
 * Each implementation handles the specific format requirements of each bank.
 *
 * @author CashBee Team
 */
public interface BatchTransferExcelGenerator {

    /**
     * Generate Excel file from list of transfer rows.
     *
     * @param rows List of transfer data rows
     * @return Excel file as byte array
     */
    byte[] generate(List<BatchTransferRow> rows);

    /**
     * Get the bank template type this generator supports.
     *
     * @return BankTemplate enum value
     */
    BankTemplate getTemplate();
}
