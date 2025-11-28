package com.cashbee.presentation.controller;

import com.cashbee.application.dto.batch.ExportBatchTransferRequest;
import com.cashbee.application.dto.batch.ExportBatchTransferResponse;
import com.cashbee.application.service.email.BatchTransferEmailService;
import com.cashbee.application.service.usecase.CompleteBatchTransferUseCase;
import com.cashbee.application.service.usecase.ExportBatchTransferUseCase;
import com.cashbee.application.service.usecase.GenerateBatchTransferFileUseCase;
import com.cashbee.application.service.usecase.GetBatchExportHistoryUseCase;
import com.cashbee.domain.enums.BankTemplate;
import com.cashbee.domain.model.BatchTransferExport;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for batch transfer operations.
 *
 * Provides endpoints for:
 * - Creating batch export metadata
 * - Downloading Excel files
 * - Sending files via email
 * - Viewing export history
 *
 * All endpoints require ADMIN role.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/admin/batch-transfer")
@RequiredArgsConstructor

@Tag(name = "Batch Transfer", description = "Batch transfer management APIs")
public class BatchTransferController {
    private static final Logger log = LoggerFactory.getLogger(BatchTransferController.class);

    private final ExportBatchTransferUseCase exportBatchTransferUseCase;
    private final GenerateBatchTransferFileUseCase generateBatchTransferFileUseCase;
    private final GetBatchExportHistoryUseCase getBatchExportHistoryUseCase;
    private final CompleteBatchTransferUseCase completeBatchTransferUseCase;
    private final BatchTransferEmailService emailService;

    /**
     * Create batch export metadata.
     *
     * POST /api/admin/batch-transfer/export
     *
     * Request body:
     * {
     *   "minBalance": 50000,
     *   "remarkTemplate": "Hoan tien CashBee 11/2025",
     *   "exportType": "MANUAL"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "batchCode": "BATCH_20251119_001",
     *     "fileName": "BATCH_20251119_001.xls",
     *     "totalUsers": 25,
     *     "totalAmount": 15500000,
     *     "exportedAt": "2025-11-19T14:30:00",
     *     "message": "Export completed. 25 users eligible for transfer (total: 15,500,000 VND)"
     *   }
     * }
     */
    @PostMapping("/export")
    @Operation(summary = "Create batch export metadata", description = "Create batch export metadata and calculate totals")
    public ResponseEntity<ApiResponse<ExportBatchTransferResponse>> exportBatchTransfer(
            @RequestBody(required = false) ExportBatchTransferRequest request) {

        log.info("BatchTransferController: POST /export - request: {}", request);

        // Use default request if not provided
        if (request == null) {
            request = ExportBatchTransferRequest.builder().build();
        }

        ExportBatchTransferResponse response = exportBatchTransferUseCase.execute(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Download batch transfer Excel file.
     *
     * GET /api/admin/batch-transfer/download?batchCode=BATCH_20251119_001&bankTemplate=VPBANK
     *
     * Supports two bank templates:
     * - VPBANK (default): Returns .xls file with bank_name column
     * - VIETINBANK: Returns .xlsx file with vietinbank_code (8 digits) column
     *
     * Note: File is generated fresh each time (realtime data).
     */
    @GetMapping("/download")
    @Operation(summary = "Download batch transfer file",
            description = "Generate and download Excel file for batch transfer. " +
                    "Supports templates: VPBANK (.xls) and VIETINBANK (.xlsx)")
    public ResponseEntity<byte[]> downloadBatchTransferFile(
            @RequestParam(required = false) String batchCode,
            @RequestParam(defaultValue = "VPBANK") String bankTemplate) {

        log.info("BatchTransferController: GET /download - batchCode: {}, bankTemplate: {}", batchCode, bankTemplate);

        // Parse bank template
        BankTemplate template;
        try {
            template = BankTemplate.valueOf(bankTemplate.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("BatchTransferController: Invalid bankTemplate '{}', defaulting to VPBANK", bankTemplate);
            template = BankTemplate.VPBANK;
        }

        byte[] excelBytes;
        String fileName;

        if (batchCode != null && !batchCode.isBlank()) {
            // Generate by batch code with specified template
            excelBytes = generateBatchTransferFileUseCase.generateByBatchCode(batchCode, template);
            fileName = batchCode + template.getFileExtension();
        } else {
            // Generate fresh with default criteria and specified template
            excelBytes = generateBatchTransferFileUseCase.generateFile(
                    java.math.BigDecimal.valueOf(10000),
                    null, // Use default remark
                    template
            );
            fileName = "BATCH_TRANSFER_" +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                    template.getFileExtension();
        }

        // Set content type based on file extension
        MediaType contentType = template == BankTemplate.VIETINBANK
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.APPLICATION_OCTET_STREAM;

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setContentLength(excelBytes.length);

        log.info("BatchTransferController: Returning file {} ({} bytes, template: {})",
                fileName, excelBytes.length, template);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

    /**
     * Send batch transfer file via email.
     *
     * POST /api/admin/batch-transfer/send-email
     *
     * Request body:
     * {
     *   "batchCode": "BATCH_20251119_001",
     *   "recipientEmail": "admin@cashbee.com"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Email sent successfully"
     * }
     */
    @PostMapping("/send-email")
    @Operation(summary = "Send batch transfer file via email", description = "Generate and send Excel file via email")
    public ResponseEntity<ApiResponse<String>> sendBatchTransferEmail(
            @RequestBody SendEmailRequest request) {

        log.info("BatchTransferController: POST /send-email - request: {}", request);

        // Generate file
        byte[] excelBytes;
        String fileName;
        Integer totalUsers;
        java.math.BigDecimal totalAmount;

        if (request.getBatchCode() != null && !request.getBatchCode().isBlank()) {
            // Generate by batch code and get metadata
            excelBytes = generateBatchTransferFileUseCase.generateByBatchCode(request.getBatchCode());
            fileName = request.getBatchCode() + ".xls";

            // Get metadata from database (we would need to add this logic)
            // For now, use placeholders
            totalUsers = 0;
            totalAmount = java.math.BigDecimal.ZERO;

        } else {
            // Generate fresh
            excelBytes = generateBatchTransferFileUseCase.generateFile(
                    java.math.BigDecimal.valueOf(50000),
                    null
            );
            fileName = "BATCH_TRANSFER_" +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                    ".xls";
            totalUsers = 0;
            totalAmount = java.math.BigDecimal.ZERO;
        }

        // Send email
        emailService.sendBatchTransferFile(
                fileName,
                excelBytes,
                totalUsers,
                totalAmount,
                request.getRecipientEmail()
        );

        return ResponseEntity.ok(ApiResponse.success("Email sent successfully to " +
                (request.getRecipientEmail() != null ? request.getRecipientEmail() : "admin email")));
    }

    /**
     * Get batch export history.
     *
     * GET /api/admin/batch-transfer/history?page=0&size=10
     *
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "content": [...],
     *     "totalElements": 100,
     *     "totalPages": 10,
     *     "size": 10,
     *     "number": 0
     *   }
     * }
     */
    @GetMapping("/history")
    @Operation(summary = "Get batch export history", description = "View all batch export history with pagination")
    public ResponseEntity<ApiResponse<Page<ExportBatchTransferResponse>>> getBatchExportHistory(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        log.info("BatchTransferController: GET /history - page: {}", pageable.getPageNumber());

        Page<ExportBatchTransferResponse> history = getBatchExportHistoryUseCase.execute(pageable);

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Get batch export history by type.
     *
     * GET /api/admin/batch-transfer/history/MANUAL?page=0&size=10
     */
    @GetMapping("/history/{exportType}")
    @Operation(summary = "Get batch export history by type", description = "View batch export history filtered by type")
    public ResponseEntity<ApiResponse<Page<ExportBatchTransferResponse>>> getBatchExportHistoryByType(
            @PathVariable String exportType,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        log.info("BatchTransferController: GET /history/{} - page: {}", exportType, pageable.getPageNumber());

        Page<ExportBatchTransferResponse> history = getBatchExportHistoryUseCase.executeByType(exportType, pageable);

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Complete batch transfer after admin has transferred money.
     *
     * POST /api/admin/batch-transfer/{batchCode}/complete
     *
     * This endpoint should be called after admin has:
     * 1. Downloaded the Excel file
     * 2. Imported it to bank's web interface
     * 3. Bank has successfully transferred money to all users
     *
     * What this does:
     * - Deducts balance from all users in the batch
     * - Creates transaction records for each user
     * - Marks batch as COMPLETED
     *
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "batchCode": "BATCH_20251119_001",
     *     "status": "COMPLETED",
     *     "message": "Batch completed. 25 users processed."
     *   }
     * }
     */
    @PostMapping("/{batchCode}/complete")
    @Operation(summary = "Complete batch transfer", description = "Mark batch as completed and deduct balance from all users")
    public ResponseEntity<ApiResponse<CompleteBatchResponse>> completeBatchTransfer(
            @PathVariable String batchCode) {

        log.info("BatchTransferController: POST /{}/complete", batchCode);

        BatchTransferExport completedBatch = completeBatchTransferUseCase.execute(batchCode);

        CompleteBatchResponse response = new CompleteBatchResponse(
                completedBatch.getBatchCode(),
                completedBatch.getStatus().name(),
                String.format("Batch completed. %d users processed.", completedBatch.getTotalUsers())
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Response DTO for complete batch endpoint.
     */
    public record CompleteBatchResponse(String batchCode, String status, String message) {}

    /**
     * Request DTO for send email endpoint.
     */
    public record SendEmailRequest(String batchCode, String recipientEmail) {
        public String getBatchCode() {
            return batchCode;
        }

        public String getRecipientEmail() {
            return recipientEmail;
        }
    }
}
