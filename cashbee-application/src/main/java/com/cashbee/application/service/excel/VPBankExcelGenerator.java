package com.cashbee.application.service.excel;

import com.cashbee.application.dto.batch.BatchTransferRow;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.BankTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service to generate VPBank batch transfer Excel file (.xls format).
 * <p>
 * VPBank template structure (7 columns):
 * - Col 0: # (STT)
 * - Col 1: Số Tài Khoản (Account Number)
 * - Col 2: Tên Tài Khoản (Account Name) - UPPERCASE, no accents
 * - Col 3: Số Tiền (Amount) - integer, no formatting
 * - Col 4: Ngân Hàng Hưởng (Bank) - bank code like TCB, VCB
 * - Col 5: BANKID - VPBank internal bank ID (integer)
 * - Col 6: Nội Dung (Remark) - no accents
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VPBankExcelGenerator implements BatchTransferExcelGenerator {

    private static final String TEMPLATE_PATH = "templates/vpbank_template.xls";
    private static final String DEFAULT_REMARK = "CASHBEE HOAN TIEN";

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

        try {
            // Load template file
            HSSFWorkbook workbook = loadTemplate();

            // Get or create sheet
            HSSFSheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                sheet = workbook.createSheet("Batch");
            }

            // Clear existing data (keep header)
            clearExistingData(sheet);

            // Create header if not exists
            if (sheet.getPhysicalNumberOfRows() == 0) {
                createHeader(sheet, workbook);
            }

            // Add data rows
            addDataRows(sheet, rows, workbook);

            // Auto-size columns
            autoSizeColumns(sheet);

            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            byte[] excelBytes = outputStream.toByteArray();
            log.info("VPBankExcelGenerator: Generated Excel file ({} bytes)", excelBytes.length);

            return excelBytes;

        } catch (Exception e) {
            log.error("VPBankExcelGenerator: Failed to generate Excel file", e);
            throw new BusinessException("EXCEL_GENERATION_FAILED",
                    "Failed to generate Excel file: " + e.getMessage());
        }
    }

    /**
     * Load template file from resources.
     */
    private HSSFWorkbook loadTemplate() {
        log.debug("VPBankExcelGenerator: Loading template from {}", TEMPLATE_PATH);

        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            if (!resource.exists()) {
                log.warn("VPBankExcelGenerator: Template not found, creating new workbook");
                return new HSSFWorkbook();
            }

            try (InputStream inputStream = resource.getInputStream()) {
                return new HSSFWorkbook(inputStream);
            }
        } catch (Exception e) {
            log.warn("VPBankExcelGenerator: Failed to load template, creating new workbook: {}", e.getMessage());
            return new HSSFWorkbook();
        }
    }

    /**
     * Clear existing data rows (keep header).
     */
    private void clearExistingData(HSSFSheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum > 0) {
            for (int i = lastRowNum; i > 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    sheet.removeRow(row);
                }
            }
        }
    }

    /**
     * Create header row (7 columns matching VPBank template).
     */
    private void createHeader(HSSFSheet sheet, HSSFWorkbook workbook) {
        log.debug("VPBankExcelGenerator: Creating header row");

        Row headerRow = sheet.createRow(0);

        // Create header cell style (bold)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 7 columns matching VPBank template
        String[] headers = {
                "#",
                "Số Tài Khoản (Account Number)",
                "Tên Tài Khoản (Account Name)",
                "Số Tiền (Amount)",
                "Ngân Hàng Hưởng (Bank)",
                "BANKID",
                "Nội Dung (Remark)"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Add data rows to sheet (7 columns).
     * Account number is stored as TEXT to preserve leading zeros.
     */
    private void addDataRows(HSSFSheet sheet, List<BatchTransferRow> rows, HSSFWorkbook workbook) {
        log.debug("VPBankExcelGenerator: Adding {} data rows", rows.size());

        // Create text style for account number (preserves leading zeros)
        HSSFCellStyle textStyle = createTextStyle(workbook);

        int rowNum = 1; // Start from row 1 (row 0 is header)

        for (BatchTransferRow data : rows) {
            Row row = sheet.createRow(rowNum++);

            // Col 0: # (STT) - NUMBER
            Cell sttCell = row.createCell(0);
            sttCell.setCellValue(data.getStt());

            // Col 1: Số Tài Khoản - TEXT (to preserve leading zeros like "0123456789")
            Cell accountNumberCell = row.createCell(1);
            accountNumberCell.setCellValue(data.getAccountNumber());
            accountNumberCell.setCellStyle(textStyle);

            // Col 2: Tên Tài Khoản - TEXT (UPPERCASE, no accents)
            Cell accountNameCell = row.createCell(2);
            String processedName = processAccountName(data.getAccountName());
            accountNameCell.setCellValue(processedName);

            // Col 3: Số Tiền - NUMBER (integer, no formatting)
            Cell amountCell = row.createCell(3);
            long amountValue = data.getAmount().setScale(0, RoundingMode.FLOOR).longValue();
            amountCell.setCellValue(amountValue);

            // Col 4: Ngân Hàng Hưởng - TEXT (bank code like TCB, VCB)
            Cell bankCell = row.createCell(4);
            bankCell.setCellValue(data.getBankCode());

            // Col 5: BANKID - NUMBER (VPBank internal bank ID)
            Cell bankIdCell = row.createCell(5);
            bankIdCell.setCellValue(data.getVpbankId());

            // Col 6: Nội Dung - TEXT (no accents, fixed content)
            Cell remarkCell = row.createCell(6);
            remarkCell.setCellValue(DEFAULT_REMARK);
        }
    }

    /**
     * Process account name for VPBank requirements:
     * 1. Remove Vietnamese accents
     * 2. Convert to UPPERCASE
     * 3. Remove special characters like "&"
     * <p>
     * Example: "Nguyễn Văn A" -> "NGUYEN VAN A"
     */
    private String processAccountName(String accountName) {
        if (accountName == null || accountName.isEmpty()) {
            return "";
        }

        // Step 1: Remove Vietnamese accents
        String processed = removeVietnameseAccents(accountName);

        // Step 2: Convert to uppercase
        processed = processed.toUpperCase();

        // Step 3: Remove special characters
        processed = processed.replace("&", " ");

        // Step 4: Clean up extra whitespace
        processed = processed.replaceAll("\\s+", " ").trim();

        return processed;
    }

    /**
     * Remove Vietnamese accents from string.
     * <p>
     * Example: "Nguyễn Văn" -> "Nguyen Van"
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
        // Format "@" = Text format, preserves leading zeros
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        return style;
    }

    /**
     * Auto-size all 7 columns.
     */
    private void autoSizeColumns(HSSFSheet sheet) {
        for (int i = 0; i < 7; i++) {
            try {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            } catch (Exception e) {
                log.warn("VPBankExcelGenerator: Failed to auto-size column {}", i);
            }
        }
    }
}
