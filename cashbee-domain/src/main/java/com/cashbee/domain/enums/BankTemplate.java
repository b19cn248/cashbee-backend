package com.cashbee.domain.enums;

/**
 * Enum for bank template types used in batch transfer file generation.
 *
 * Each bank has different file format requirements:
 * - VPBANK: Uses .xls format with bank_name column
 * - VIETINBANK: Uses .xlsx format with vietinbank_code (8 digits) column
 *
 * @author CashBee Team
 */
public enum BankTemplate {

    /**
     * VPBank template format.
     * - File extension: .xls (HSSFWorkbook)
     * - Bank column: bank_name (e.g., "Vietcombank", "BIDV")
     */
    VPBANK("vpbank_template.xls", ".xls"),

    /**
     * VietinBank template format.
     * - File extension: .xlsx (XSSFWorkbook)
     * - Bank column: vietinbank_code (e.g., "01309001", "01202001")
     * - Special: If transfer to VietinBank, use "VietinBank" instead of code
     * - Account name: Must be uppercase, no Vietnamese accents, no "&" character
     */
    VIETINBANK("vietinbank_template.xlsx", ".xlsx");

    private final String templateFileName;
    private final String fileExtension;

    BankTemplate(String templateFileName, String fileExtension) {
        this.templateFileName = templateFileName;
        this.fileExtension = fileExtension;
    }

    public String getTemplateFileName() {
        return templateFileName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getTemplatePath() {
        return "templates/" + templateFileName;
    }
}
