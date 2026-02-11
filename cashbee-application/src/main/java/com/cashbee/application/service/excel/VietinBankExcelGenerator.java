package com.cashbee.application.service.excel;

import com.cashbee.application.dto.batch.BatchTransferRow;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.BankTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service to generate VietinBank batch transfer Excel file (.xlsx format).
 *
 * VietinBank specific requirements:
 * 1. File format: .xlsx (XSSFWorkbook)
 * 2. Account name: Must be UPPERCASE, no Vietnamese accents, no "&" character
 * 3. Bank column: Use vietinbank_code (8 digits) or "VietinBank" for internal transfer
 * 4. All text cells should be plain text format
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VietinBankExcelGenerator implements BatchTransferExcelGenerator {

    private static final String TEMPLATE_PATH = "templates/vietinbank_template.xlsx";
    private static final String VIETINBANK_INTERNAL = "VietinBank";

    @Override
    public BankTemplate getTemplate() {
        return BankTemplate.VIETINBANK;
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
        log.info("VietinBankExcelGenerator: Generating Excel file with {} rows", rows.size());

        try {
            // Load template file
            XSSFWorkbook workbook = loadTemplate();

            // Get or create sheet
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                sheet = workbook.createSheet("ChuyenKhoan");
            }

            // Clear existing data (keep header row)
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
            log.info("VietinBankExcelGenerator: Generated Excel file ({} bytes)", excelBytes.length);

            return excelBytes;

        } catch (Exception e) {
            log.error("VietinBankExcelGenerator: Failed to generate Excel file", e);
            throw new BusinessException("EXCEL_GENERATION_FAILED",
                    "Failed to generate VietinBank Excel file: " + e.getMessage());
        }
    }

    /**
     * Load template file from resources.
     */
    private XSSFWorkbook loadTemplate() throws Exception {
        log.debug("VietinBankExcelGenerator: Loading template from {}", TEMPLATE_PATH);

        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            if (!resource.exists()) {
                log.warn("VietinBankExcelGenerator: Template not found, creating new workbook");
                return new XSSFWorkbook();
            }

            try (InputStream inputStream = resource.getInputStream()) {
                return new XSSFWorkbook(inputStream);
            }
        } catch (Exception e) {
            log.warn("VietinBankExcelGenerator: Failed to load template, creating new workbook: {}", e.getMessage());
            return new XSSFWorkbook();
        }
    }

    /**
     * Clear existing data rows (keep header).
     */
    private void clearExistingData(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum > 0) {
            // Remove all rows except header (row 0)
            for (int i = lastRowNum; i > 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    sheet.removeRow(row);
                }
            }
        }
    }

    /**
     * Create header row if not exists.
     * VietinBank format columns:
     * A: STT
     * B: So Tai Khoan (*)
     * C: Ten Nguoi Huong (*)
     * D: So Tien (*)
     * E: Ngan Hang Thu Huong (*)
     * F: Noi Dung
     */
    private void createHeader(Sheet sheet, Workbook workbook) {
        log.debug("VietinBankExcelGenerator: Creating header row");

        Row headerRow = sheet.createRow(0);

        // Create header cell style (bold)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Create header cells (VietinBank format)
        String[] headers = {
                "STT",
                "So Tai Khoan (*)",
                "Ten Nguoi Huong (*)",
                "So Tien (*)",
                "Ngan Hang Thu Huong (*)",
                "Noi Dung"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Add data rows to sheet.
     */
    private void addDataRows(Sheet sheet, List<BatchTransferRow> rows, Workbook workbook) {
        log.debug("VietinBankExcelGenerator: Adding {} data rows", rows.size());

        // Create cell styles
        CellStyle numberStyle = createNumberStyle(workbook);
        CellStyle amountStyle = createAmountStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);

        int rowNum = 1; // Start from row 1 (row 0 is header)

        for (BatchTransferRow data : rows) {
            Row row = sheet.createRow(rowNum++);

            // Column 0: STT
            Cell sttCell = row.createCell(0);
            sttCell.setCellValue(data.getStt());
            sttCell.setCellStyle(numberStyle);

            // Column 1: Account Number (as text to preserve leading zeros)
            Cell accountNumberCell = row.createCell(1);
            accountNumberCell.setCellValue(data.getAccountNumber());
            accountNumberCell.setCellStyle(textStyle);

            // Column 2: Account Name (UPPERCASE, no accents, no "&")
            Cell accountNameCell = row.createCell(2);
            String processedName = processAccountName(data.getAccountName());
            accountNameCell.setCellValue(processedName);
            accountNameCell.setCellStyle(textStyle);

            // Column 3: Amount
            Cell amountCell = row.createCell(3);
            amountCell.setCellValue(data.getAmount().doubleValue());
            amountCell.setCellStyle(amountStyle);

            // Column 4: Bank Code (VietinBank format)
            Cell bankCell = row.createCell(4);
            String bankCode = processBankCode(data.getVietinbankCode());
            bankCell.setCellValue(bankCode);
            bankCell.setCellStyle(textStyle);

            // Column 5: Remark (no accents for compatibility)
            Cell remarkCell = row.createCell(5);
            String processedRemark = removeVietnameseAccents(data.getRemark());
            remarkCell.setCellValue(processedRemark);
            remarkCell.setCellStyle(textStyle);
        }
    }

    /**
     * Process account name for VietinBank requirements:
     * 1. Remove "&" character
     * 2. Remove Vietnamese accents
     * 3. Convert to UPPERCASE
     *
     * Example: "Nguyễn Văn A & B" -> "NGUYEN VAN A B"
     */
    private String processAccountName(String accountName) {
        if (accountName == null || accountName.isEmpty()) {
            return "";
        }

        // Step 1: Remove "&" character (VietinBank requirement)
        String processed = accountName.replace("&", " ");

        // Step 2: Remove Vietnamese accents
        processed = removeVietnameseAccents(processed);

        // Step 3: Convert to uppercase
        processed = processed.toUpperCase();

        // Step 4: Remove extra whitespace
        processed = processed.replaceAll("\\s+", " ").trim();

        return processed;
    }

    /**
     * Remove Vietnamese accents from string.
     *
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

        // Handle special Vietnamese characters that NFD doesn't fully decompose
        result = result.replace("đ", "d").replace("Đ", "D");

        return result;
    }

    /**
     * Process bank code for VietinBank format.
     * - If vietinbankCode is "VietinBank" -> return "VietinBank" (internal transfer)
     * - Otherwise return the 8-digit code
     */
    private String processBankCode(String vietinbankCode) {
        if (vietinbankCode == null || vietinbankCode.isEmpty()) {
            return "";
        }

        // If it's VietinBank internal transfer
        if (VIETINBANK_INTERNAL.equalsIgnoreCase(vietinbankCode)) {
            return VIETINBANK_INTERNAL;
        }

        // Return the 8-digit code as-is
        return vietinbankCode;
    }

    /**
     * Create number cell style (for STT).
     */
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Create amount cell style (for money).
     */
    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Format number with thousand separator, no decimals for VND
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));

        return style;
    }

    /**
     * Create text cell style.
     */
    private CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Set as text format to preserve leading zeros in account numbers
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));

        return style;
    }

    /**
     * Auto-size all columns.
     */
    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 6; i++) {
            try {
                sheet.autoSizeColumn(i);
                // Add some padding
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            } catch (Exception e) {
                // Ignore auto-size errors
                log.warn("VietinBankExcelGenerator: Failed to auto-size column {}", i);
            }
        }
    }
}
