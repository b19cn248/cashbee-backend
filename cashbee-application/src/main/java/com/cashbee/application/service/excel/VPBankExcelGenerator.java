package com.cashbee.application.service.excel;

import com.cashbee.application.dto.batch.BatchTransferRow;
import com.cashbee.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Service to generate VPBank batch transfer Excel file (.xls format).
 *
 * Sử dụng template file từ resources/templates/vpbank_template.xls
 * để đảm bảo format đúng theo yêu cầu của VPBank.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VPBankExcelGenerator {

    private static final String TEMPLATE_PATH = "templates/vpbank_template.xls";

    /**
     * Generate Excel file from list of transfer rows.
     *
     * @param rows List of transfer rows
     * @return Excel file as byte array
     * @throws BusinessException if generation fails
     */
    public byte[] generate(List<BatchTransferRow> rows) {
        log.info("ExcelGenerator: Generating Excel file with {} rows", rows.size());

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
            log.info("ExcelGenerator: Generated Excel file ({} bytes)", excelBytes.length);

            return excelBytes;

        } catch (Exception e) {
            log.error("ExcelGenerator: Failed to generate Excel file", e);
            throw new BusinessException("EXCEL_GENERATION_FAILED",
                    "Failed to generate Excel file: " + e.getMessage());
        }
    }

    /**
     * Load template file from resources.
     */
    private HSSFWorkbook loadTemplate() throws Exception {
        log.debug("ExcelGenerator: Loading template from {}", TEMPLATE_PATH);

        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            if (!resource.exists()) {
                log.warn("ExcelGenerator: Template not found, creating new workbook");
                return new HSSFWorkbook();
            }

            try (InputStream inputStream = resource.getInputStream()) {
                return new HSSFWorkbook(inputStream);
            }
        } catch (Exception e) {
            log.warn("ExcelGenerator: Failed to load template, creating new workbook: {}", e.getMessage());
            return new HSSFWorkbook();
        }
    }

    /**
     * Clear existing data rows (keep header).
     */
    private void clearExistingData(HSSFSheet sheet) {
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
     */
    private void createHeader(HSSFSheet sheet, HSSFWorkbook workbook) {
        log.debug("ExcelGenerator: Creating header row");

        Row headerRow = sheet.createRow(0);

        // Create header cell style (bold)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Create header cells
        String[] headers = {
                "#",
                "Số Tài Khoản (Account Number)",
                "Tên Tài Khoản (Account Name)",
                "Số Tiền (Amount)",
                "Ngân Hàng Hưởng (Bank)",
                "Nội Dung (Remark)"
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
    private void addDataRows(HSSFSheet sheet, List<BatchTransferRow> rows, HSSFWorkbook workbook) {
        log.debug("ExcelGenerator: Adding {} data rows", rows.size());

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

            // Column 1: Account Number
            Cell accountNumberCell = row.createCell(1);
            accountNumberCell.setCellValue(data.getAccountNumber());
            accountNumberCell.setCellStyle(textStyle);

            // Column 2: Account Name
            Cell accountNameCell = row.createCell(2);
            accountNameCell.setCellValue(data.getAccountName());
            accountNameCell.setCellStyle(textStyle);

            // Column 3: Amount
            Cell amountCell = row.createCell(3);
            amountCell.setCellValue(data.getAmount().doubleValue());
            amountCell.setCellStyle(amountStyle);

            // Column 4: Bank Name
            Cell bankCell = row.createCell(4);
            bankCell.setCellValue(data.getBankName());
            bankCell.setCellStyle(textStyle);

            // Column 5: Remark
            Cell remarkCell = row.createCell(5);
            remarkCell.setCellValue(data.getRemark());
            remarkCell.setCellStyle(textStyle);
        }
    }

    /**
     * Create number cell style (for STT).
     */
    private CellStyle createNumberStyle(HSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Create amount cell style (for money).
     */
    private CellStyle createAmountStyle(HSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Format number with thousand separator
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));

        return style;
    }

    /**
     * Create text cell style.
     */
    private CellStyle createTextStyle(HSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Auto-size all columns.
     */
    private void autoSizeColumns(HSSFSheet sheet) {
        for (int i = 0; i < 6; i++) {
            try {
                sheet.autoSizeColumn(i);
                // Add some padding
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            } catch (Exception e) {
                // Ignore auto-size errors
                log.warn("ExcelGenerator: Failed to auto-size column {}", i);
            }
        }
    }
}
