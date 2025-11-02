package com.cashbee.application.dto.affiliate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * Request DTO for importing affiliate orders from CSV file.
 *
 * Admin uploads daily CSV file from Shopee affiliate portal.
 * System processes the file and creates orders.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportOrdersRequest {

    /**
     * CSV file input stream.
     * Expected format: Shopee affiliate commission report CSV.
     */
    @NotNull(message = "CSV file input stream is required")
    private InputStream fileInputStream;

    /**
     * Original file name.
     */
    @NotBlank(message = "File name is required")
    private String fileName;

    /**
     * Platform code (e.g., "shopee", "lazada").
     * Defaults to "shopee" if not specified.
     */
    private String platformCode;

    /**
     * Admin user ID who is importing the file.
     */
    @NotNull(message = "Admin user ID is required")
    private Long importedBy;

    /**
     * Whether to skip duplicate orders.
     * If true, orders with existing order IDs will be skipped.
     * If false, duplicate orders will cause error.
     */
    @Builder.Default
    private Boolean skipDuplicates = true;

    /**
     * Whether to match orders with clicks automatically.
     * If true, system will try to match orders with AffiliateClick records.
     * If false, matching will be skipped (can be done later manually).
     */
    @Builder.Default
    private Boolean autoMatch = true;
}
