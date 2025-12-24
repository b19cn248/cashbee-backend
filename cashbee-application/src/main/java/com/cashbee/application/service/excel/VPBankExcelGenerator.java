package com.cashbee.application.service.excel;

import com.cashbee.application.dto.batch.BatchTransferRow;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.BankTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service to generate VPBank batch transfer Excel file (.xls format).
 *
 * <p>Generates file matching VPBank TTTN (Thanh Toan Trong Nuoc) template with 11 columns:
 * <ol>
 *   <li>STT - Row number</li>
 *   <li>Account - Bank account number</li>
 *   <li>Currency - Currency code (VND)</li>
 *   <li>Ben_Name - Beneficiary name (uppercase, no Vietnamese)</li>
 *   <li>Bank_Code - VPBank 9-digit code (empty for internal VPBank)</li>
 *   <li>Bank_Name - Bank short name</li>
 *   <li>Branch_Name - Branch (empty for internal VPBank)</li>
 *   <li>City_Name - City (empty for internal VPBank)</li>
 *   <li>Amount - Transfer amount (integer for VND)</li>
 *   <li>Details - Payment description (no Vietnamese)</li>
 *   <li>Charges - Fee type: OUR or BEN</li>
 * </ol>
 *
 * <p>Rules from VPBank TTTN_HUONG DAN:
 * <ul>
 *   <li>No Vietnamese characters allowed</li>
 *   <li>No special characters (only: SPACE A-Za-z0-9.+-)(,)</li>
 *   <li>No empty rows</li>
 *   <li>VND amounts must be integers (no decimal point)</li>
 *   <li>For internal VPBank transfers: Bank_Code, Branch_Name, City_Name are empty</li>
 * </ul>
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VPBankExcelGenerator implements BatchTransferExcelGenerator {

    /**
     * Allowed characters for VPBank fields (from TTTN_HUONG DAN).
     */
    private static final String ALLOWED_CHARS_REGEX = "[^A-Za-z0-9 .+\\-)(,]";
    private static final Pattern ALLOWED_CHARS_PATTERN = Pattern.compile(ALLOWED_CHARS_REGEX);

    /**
     * Header names matching VPBank TTTN template exactly.
     */
    private static final String[] HEADERS = {
            "STT",
            "Account",
            "Currency",
            "Ben_Name",
            "Bank_Code",
            "Bank_Name",
            "Branch_Name",
            "City_Name",
            "Amount",
            "Details",
            "Charges"
    };

    private static final int COLUMN_COUNT = 11;

    @Override
    public BankTemplate getTemplate() {
        return BankTemplate.VPBANK;
    }

    /**
     * Generate Excel file from list of transfer rows.
     *
     * @param rows List of transfer rows
     * @return Excel file as byte array
     * @throws BusinessException if generation fails
     */
    @Override
    public byte[] generate(List<BatchTransferRow> rows) {
        log.info("VPBankExcelGenerator: Generating Excel file with {} rows", rows.size());

        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            // Create sheet named "TTTN" matching VPBank template
            HSSFSheet sheet = workbook.createSheet("TTTN");

            // Create header row
            createHeaderRow(sheet, workbook);

            // Create data rows
            createDataRows(sheet, workbook, rows);

            // Auto-size columns for better readability
            autoSizeColumns(sheet);

            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            byte[] excelBytes = outputStream.toByteArray();
            log.info("VPBankExcelGenerator: Generated Excel file ({} bytes, {} rows)",
                    excelBytes.length, rows.size());

            return excelBytes;

        } catch (Exception e) {
            log.error("VPBankExcelGenerator: Failed to generate Excel file", e);
            throw new BusinessException("EXCEL_GENERATION_FAILED",
                    "Failed to generate VPBank Excel file: " + e.getMessage());
        }
    }

    /**
     * Create header row with VPBank TTTN column names.
     */
    private void createHeaderRow(HSSFSheet sheet, HSSFWorkbook workbook) {
        Row headerRow = sheet.createRow(0);

        // Create header style (bold, centered)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        log.debug("VPBankExcelGenerator: Created header row with {} columns", HEADERS.length);
    }

    /**
     * Create data rows from BatchTransferRow list.
     */
    private void createDataRows(HSSFSheet sheet, HSSFWorkbook workbook, List<BatchTransferRow> rows) {
        // Create text style for account number (preserves leading zeros)
        HSSFCellStyle textStyle = createTextStyle(workbook);

        int rowNum = 1; // Start from row 1 (row 0 is header)

        for (BatchTransferRow data : rows) {
            Row row = sheet.createRow(rowNum++);
            populateDataRow(row, data, textStyle);
        }

        log.debug("VPBankExcelGenerator: Created {} data rows", rows.size());
    }

    /**
     * Populate a single data row with values from BatchTransferRow.
     *
     * <p>Column mapping:
     * <pre>
     * 0: STT          - Row number
     * 1: Account      - Bank account number (text format)
     * 2: Currency     - VND
     * 3: Ben_Name     - Beneficiary name (processed)
     * 4: Bank_Code    - VPBank 9-digit code (empty for internal VPBank only)
     * 5: Bank_Name    - Bank short name
     * 6: Branch_Name  - "ALL" for external banks, empty for internal VPBank
     * 7: City_Name    - "ALL" for external banks, empty for internal VPBank
     * 8: Amount       - Integer for VND
     * 9: Details      - Payment description (processed)
     * 10: Charges     - OUR or BEN
     * </pre>
     *
     * <p>Important: According to VPBank TTTN guide:
     * - Branch_Name and City_Name can only be empty for internal VPBank transfers
     * - For external banks, use "ALL" when specific branch/city is unknown
     * - Bank_Code must be provided for external banks (9-digit code)
     */
    private void populateDataRow(Row row, BatchTransferRow data, CellStyle textStyle) {
        boolean isInternalVPBank = isInternalVPBankTransfer(data);

        // Column 0: STT (Number)
        row.createCell(0).setCellValue(data.getStt());

        // Column 1: Account (Text - preserves leading zeros)
        Cell accountCell = row.createCell(1);
        accountCell.setCellValue(sanitizeAccountNumber(data.getAccountNumber()));
        accountCell.setCellStyle(textStyle);

        // Column 2: Currency (Text)
        String currency = data.getCurrency() != null ? data.getCurrency() : "VND";
        row.createCell(2).setCellValue(currency);

        // Column 3: Ben_Name (Text - processed: uppercase, no Vietnamese)
        row.createCell(3).setCellValue(processName(data.getAccountName()));

        // Column 4: Bank_Code (Text - empty ONLY for internal VPBank)
        String bankCode = isInternalVPBank ? "" : nullToEmpty(data.getVpbankCode());
        row.createCell(4).setCellValue(bankCode);

        // Column 5: Bank_Name (Text - use short name, processed)
        String bankName = isInternalVPBank ? "VPBANK" : extractBankShortName(data.getBankName(), data.getBankCode());
        row.createCell(5).setCellValue(bankName);

        // Column 6: Branch_Name (Text - "ALL" for external banks, empty for internal VPBank)
        // VPBank requires Branch_Name for external transfers; "ALL" means all branches
        String branchName = isInternalVPBank ? "" : "ALL";
        row.createCell(6).setCellValue(branchName);

        // Column 7: City_Name (Text - valid city name for external banks, empty for internal VPBank)
        // VPBank requires City_Name from valid city list (64 provinces/cities in Vietnam)
        // Using "Ho Chi Minh" as default since it's the largest city with most bank branches
        String cityName = isInternalVPBank ? "" : "Ho Chi Minh";
        row.createCell(7).setCellValue(cityName);

        // Column 8: Amount (Number - integer for VND)
        Cell amountCell = row.createCell(8);
        BigDecimal amount = data.getAmount() != null ? data.getAmount() : BigDecimal.ZERO;
        if ("VND".equals(currency) || "JPY".equals(currency)) {
            // VND and JPY: integer only (no decimal)
            amountCell.setCellValue(amount.setScale(0, RoundingMode.FLOOR).longValue());
        } else {
            // Other currencies: max 2 decimal places
            amountCell.setCellValue(amount.setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        // Column 9: Details (Text - processed: no Vietnamese)
        row.createCell(9).setCellValue(processDetails(data.getRemark()));

        // Column 10: Charges (Text - OUR or BEN)
        String charges = data.getCharges() != null ? data.getCharges() : "OUR";
        row.createCell(10).setCellValue(charges);
    }

    /**
     * Check if this is an internal VPBank transfer.
     * Internal transfers are when the beneficiary bank is VPBank itself.
     *
     * @param data Transfer row data
     * @return true if transferring to a VPBank account
     */
    private boolean isInternalVPBankTransfer(BatchTransferRow data) {
        // Check by bank code first (most reliable)
        String bankCode = data.getBankCode();
        if (bankCode != null && bankCode.toUpperCase().contains("VPBANK")) {
            return true;
        }

        // Also check bank name as fallback
        String bankName = data.getBankName();
        if (bankName != null && bankName.toUpperCase().contains("VPBANK")) {
            return true;
        }

        return false;
    }

    /**
     * Extract short bank name for Bank_Name column.
     * VPBank template expects short names like "VIETCOMBANK", "TECHCOMBANK", not full names.
     *
     * @param fullBankName Full bank name from database
     * @param bankCode Bank code as fallback
     * @return Short bank name for VPBank template
     */
    private String extractBankShortName(String fullBankName, String bankCode) {
        if (fullBankName == null || fullBankName.isEmpty()) {
            return bankCode != null ? bankCode.toUpperCase() : "";
        }

        // Try to extract short name from parentheses: "... (VIETCOMBANK)" -> "VIETCOMBANK"
        int startParen = fullBankName.lastIndexOf('(');
        int endParen = fullBankName.lastIndexOf(')');
        if (startParen >= 0 && endParen > startParen) {
            String shortName = fullBankName.substring(startParen + 1, endParen).trim();
            return processName(shortName);
        }

        // If no parentheses, use the full name processed
        return processName(fullBankName);
    }

    /**
     * Sanitize account number: remove special characters, keep only alphanumeric.
     */
    private String sanitizeAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return "";
        }
        // Remove any non-alphanumeric characters
        return accountNumber.replaceAll("[^A-Za-z0-9]", "");
    }

    /**
     * Process name/text field for VPBank requirements:
     * 1. Remove Vietnamese accents
     * 2. Convert to UPPERCASE
     * 3. Remove disallowed special characters
     * 4. Clean up extra whitespace
     *
     * <p>Example: "Nguyễn Văn A" → "NGUYEN VAN A"
     */
    private String processName(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Step 1: Remove Vietnamese accents
        String processed = removeVietnameseAccents(text);

        // Step 2: Convert to uppercase
        processed = processed.toUpperCase();

        // Step 3: Remove disallowed characters
        processed = ALLOWED_CHARS_PATTERN.matcher(processed).replaceAll("");

        // Step 4: Clean up extra whitespace
        processed = processed.replaceAll("\\s+", " ").trim();

        return processed;
    }

    /**
     * Process payment details for VPBank requirements.
     * Similar to processName but keeps original case.
     */
    private String processDetails(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Step 1: Remove Vietnamese accents
        String processed = removeVietnameseAccents(text);

        // Step 2: Remove disallowed characters
        processed = ALLOWED_CHARS_PATTERN.matcher(processed).replaceAll("");

        // Step 3: Clean up extra whitespace
        processed = processed.replaceAll("\\s+", " ").trim();

        return processed;
    }

    /**
     * Remove Vietnamese accents from string.
     *
     * <p>Uses Unicode NFD normalization to separate base characters from
     * combining diacritical marks, then removes the marks.
     *
     * <p>Example: "Nguyễn Văn" → "Nguyen Van"
     */
    private String removeVietnameseAccents(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        // Normalize to NFD form (separate base characters from accents)
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);

        // Remove all combining diacritical marks
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");

        // Handle special Vietnamese characters
        result = result.replace("đ", "d").replace("Đ", "D");

        return result;
    }

    /**
     * Create text cell style to preserve leading zeros in account numbers.
     * Uses format "@" which tells Excel to treat the value as text.
     */
    private HSSFCellStyle createTextStyle(HSSFWorkbook workbook) {
        HSSFCellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        return style;
    }

    /**
     * Auto-size all columns for better readability.
     */
    private void autoSizeColumns(HSSFSheet sheet) {
        for (int i = 0; i < COLUMN_COUNT; i++) {
            try {
                sheet.autoSizeColumn(i);
                // Add extra width for padding
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(currentWidth + 1000, 255 * 256)); // Max Excel width
            } catch (Exception e) {
                log.warn("VPBankExcelGenerator: Failed to auto-size column {}: {}", i, e.getMessage());
            }
        }
    }

    /**
     * Convert null to empty string.
     */
    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
