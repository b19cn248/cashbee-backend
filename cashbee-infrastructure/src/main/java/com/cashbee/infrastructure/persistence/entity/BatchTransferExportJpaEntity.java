package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.ExportStatus;
import com.cashbee.domain.enums.ExportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for batch_transfer_export table.
 *
 * This entity lives in the infrastructure layer and is the database representation
 * of the BatchTransferExport domain model.
 *
 * Table: batch_transfer_export
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "batch_transfer_export", indexes = {
        @Index(name = "idx_batch_transfer_export_batch_code", columnList = "batch_code"),
        @Index(name = "idx_batch_transfer_export_status", columnList = "status"),
        @Index(name = "idx_batch_transfer_export_export_type", columnList = "export_type"),
        @Index(name = "idx_batch_transfer_export_created_at", columnList = "created_at"),
        @Index(name = "idx_batch_transfer_export_exported_by", columnList = "exported_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BatchTransferExportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Batch code (unique identifier).
     * Format: BATCH_YYYYMMDD_XXX
     */
    @Column(name = "batch_code", nullable = false, unique = true, length = 50)
    private String batchCode;

    /**
     * File name (for reference).
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * Tổng số users trong batch.
     */
    @Column(name = "total_users", nullable = false)
    @Builder.Default
    private Integer totalUsers = 0;

    /**
     * Tổng số tiền cần chuyển (VND).
     */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Trạng thái export.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExportStatus status = ExportStatus.COMPLETED;

    /**
     * Loại export (MANUAL hoặc SCHEDULED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 20)
    @Builder.Default
    private ExportType exportType = ExportType.MANUAL;

    /**
     * Admin user ID (người export).
     * Null nếu là SCHEDULED job.
     */
    @Column(name = "exported_by")
    private Long exportedBy;

    /**
     * Template nội dung chuyển khoản.
     */
    @Column(name = "remark_template", length = 500)
    private String remarkTemplate;

    /**
     * Minimum balance criteria used when creating this batch.
     */
    @Column(name = "min_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minBalance = new BigDecimal("50000");

    /**
     * Timestamp when batch was created (auto-populated).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Admin user ID who completed the batch.
     * Null if batch is not yet completed.
     */
    @Column(name = "completed_by")
    private Long completedBy;

    /**
     * Timestamp when batch was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Number of items processed successfully.
     */
    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;

    /**
     * Number of items that failed processing.
     */
    @Column(name = "failed_count")
    @Builder.Default
    private Integer failedCount = 0;
}
