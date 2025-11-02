package com.cashbee.common.exception;

/**
 * Exception thrown when file processing fails (Excel/JSON import).
 *
 * @author CashBee Team
 */
public class FileProcessingException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "FILE_PROCESSING_ERROR";

    public FileProcessingException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(DEFAULT_ERROR_CODE, message, cause);
    }

    public FileProcessingException(String errorCode, String message) {
        super(errorCode, message);
    }

    public FileProcessingException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Factory method for invalid file format.
     *
     * @param expectedFormat Expected file format
     * @return FileProcessingException instance
     */
    public static FileProcessingException invalidFormat(String expectedFormat) {
        return new FileProcessingException(
            String.format("Invalid file format. Expected: %s", expectedFormat)
        );
    }

    /**
     * Factory method for empty file.
     *
     * @return FileProcessingException instance
     */
    public static FileProcessingException emptyFile() {
        return new FileProcessingException("File is empty or contains no valid data");
    }

    /**
     * Factory method for parsing error at specific row.
     *
     * @param rowNumber Row number where error occurred
     * @param reason Error reason
     * @return FileProcessingException instance
     */
    public static FileProcessingException parsingError(int rowNumber, String reason) {
        return new FileProcessingException(
            String.format("Error parsing row %d: %s", rowNumber, reason)
        );
    }
}
