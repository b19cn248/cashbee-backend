package com.cashbee.presentation.controller;

import com.cashbee.application.dto.affiliate.ImportOrdersRequest;
import com.cashbee.application.dto.affiliate.ImportOrdersResponse;
import com.cashbee.application.usecase.affiliate.ImportShopeeOrdersUseCase;
import com.cashbee.domain.enums.UpdateMode;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for Affiliate Import operations.
 * <p>
 * Endpoints: - POST /api/admin/import/orders - Import orders from CSV
 * <p>
 * Admin-only endpoints for importing affiliate orders from platform CSV files.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Affiliate Import", description = "Import affiliate orders from CSV files (Admin only)")
public class AffiliateImportController {

  private final ImportShopeeOrdersUseCase importShopeeOrdersUseCase;

  /**
   * Import affiliate orders from CSV file.
   * <p>
   * Admin uploads daily CSV file from Shopee affiliate portal. System processes the file and
   * creates orders.
   * <p>
   * Flow: 1. Admin downloads CSV from Shopee portal 2. Admin uploads CSV to this endpoint 3. System
   * parses CSV and creates orders 4. System matches orders with clicks (if tracking code present)
   * 5. Returns import statistics
   * <p>
   * CSV format: Shopee affiliate commission report Expected columns: Order ID, Shop ID, Item ID,
   * Commission, Sub_id1 (tracking code), etc.
   *
   * @param file         CSV file uploaded by admin
   * @param platformCode Platform code (default: "shopee")
   * @param importedBy   Admin user ID
   * @param updateMode   How to handle duplicate orders: SKIP or UPDATE (default: UPDATE)
   * @param autoMatch    Whether to auto-match orders with clicks (default: true)
   * @return Import statistics (success, failed, skipped, updated counts)
   */
  @PostMapping(value = "/orders", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Import affiliate orders from CSV",
      description = "Upload CSV file from Shopee affiliate portal to import orders and match with user clicks"
  )
  public ResponseEntity<ApiResponse<ImportOrdersResponse>> importOrders(
      @Parameter(description = "CSV file from Shopee affiliate portal", required = true)
      @RequestParam("file") MultipartFile file,

      @Parameter(description = "Platform code (e.g., 'shopee', 'lazada')", example = "shopee")
      @RequestParam(value = "platformCode", required = false, defaultValue = "shopee") String platformCode,

      @Parameter(description = "Admin user ID who is importing", required = true)
      @RequestParam("importedBy") Long importedBy,

      @Parameter(description = "Update mode for duplicate orders: SKIP or UPDATE (default: UPDATE)")
      @RequestParam(value = "updateMode", required = false, defaultValue = "UPDATE") UpdateMode updateMode,

      @Parameter(description = "Auto-match orders with clicks (default: true)")
      @RequestParam(value = "autoMatch", required = false, defaultValue = "true") Boolean autoMatch
  ) {
    log.info("API: Importing orders from file: {} (platform: {}, importedBy: {})",
        file.getOriginalFilename(), platformCode, importedBy);

    // Validate file
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body(
          ApiResponse.error("INVALID_FILE", "CSV file is empty")
      );
    }

    if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".csv")) {
      return ResponseEntity.badRequest().body(
          ApiResponse.error("INVALID_FILE_TYPE", "Only CSV files are supported")
      );
    }

    // Build request with InputStream
    ImportOrdersRequest request;
    try {
      request = ImportOrdersRequest.builder()
          .fileInputStream(file.getInputStream())
          .fileName(file.getOriginalFilename())
          .platformCode(platformCode)
          .importedBy(importedBy)
          .updateMode(updateMode)
          .autoMatch(autoMatch)
          .build();
    } catch (Exception e) {
      log.error("API: Failed to read file input stream", e);
      return ResponseEntity.badRequest().body(
          ApiResponse.error("FILE_READ_ERROR", "Failed to read file: " + e.getMessage())
      );
    }

    // Execute import
    ImportOrdersResponse response = importShopeeOrdersUseCase.execute(request);

    log.info(
        "API: Import completed. Batch ID: {}, Success: {}, Updated: {}, Failed: {}, Skipped: {}, Matched: {}",
        response.getBatchId(),
        response.getSuccessCount(),
        response.getUpdatedCount(),
        response.getFailedCount(),
        response.getSkippedCount(),
        response.getMatchedCount());

    // Return appropriate status based on result
    HttpStatus status = switch (response.getStatus()) {
      case COMPLETED -> HttpStatus.OK;
      case PARTIAL -> HttpStatus.MULTI_STATUS;  // 207
      case FAILED -> HttpStatus.BAD_REQUEST;
      default -> HttpStatus.ACCEPTED;  // 202 for PROCESSING
    };

    return ResponseEntity.status(status).body(
        ApiResponse.success(response, response.getMessage())
    );
  }

  /**
   * Get import batch details (for checking import status).
   * <p>
   * Returns details of a specific import batch. Can be used to check status of ongoing or completed
   * imports.
   *
   * @param batchId Import batch ID
   * @return Import batch details
   */
  @GetMapping("/batches/{batchId}")
  @Operation(
      summary = "Get import batch details",
      description = "Retrieve details of a specific import batch"
  )
  public ResponseEntity<ApiResponse<String>> getImportBatch(@PathVariable Long batchId) {
    log.info("API: Getting import batch details for ID: {}", batchId);

    // TODO: Implement GetImportBatchUseCase
    // For now, return placeholder
    return ResponseEntity.ok(
        ApiResponse.success("Import batch " + batchId, "Batch details retrieved")
    );
  }

  /**
   * Get recent import batches.
   * <p>
   * Returns list of recent import batches for admin to review.
   *
   * @param limit Number of batches to return (default: 10)
   * @return List of recent import batches
   */
  @GetMapping("/batches")
  @Operation(
      summary = "Get recent import batches",
      description = "Retrieve list of recent import batches"
  )
  public ResponseEntity<ApiResponse<String>> getRecentImportBatches(
      @Parameter(description = "Number of batches to return")
      @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit
  ) {
    log.info("API: Getting recent import batches (limit: {})", limit);

    // TODO: Implement GetRecentImportBatchesUseCase
    // For now, return placeholder
    return ResponseEntity.ok(
        ApiResponse.success("Recent import batches (limit: " + limit + ")", "Batches retrieved")
    );
  }
}
