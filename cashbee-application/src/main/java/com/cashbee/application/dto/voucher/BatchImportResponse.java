package com.cashbee.application.dto.voucher;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for batch voucher import.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportResponse {

    /**
     * Total number of vouchers received in the request.
     */
    private int totalReceived;

    /**
     * Number of vouchers successfully created.
     */
    private int created;

    /**
     * Number of vouchers updated (if duplicate handling is update).
     */
    private int updated;

    /**
     * Number of vouchers that failed to import.
     */
    private int failed;

    /**
     * List of error details for failed imports.
     */
    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    /**
     * Error detail for a failed import.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        /**
         * Index of the voucher in the batch (0-based).
         */
        private int index;

        /**
         * Error message describing why the import failed.
         */
        private String message;
    }

    /**
     * Add an error to the list.
     */
    public void addError(int index, String message) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(ImportError.builder()
            .index(index)
            .message(message)
            .build());
    }
}
