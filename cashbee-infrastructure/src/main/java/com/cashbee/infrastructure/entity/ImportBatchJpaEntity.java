package com.cashbee.infrastructure.entity;

import com.cashbee.domain.enums.ImportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity for import_batch table.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "import_batch", indexes = {
    @Index(name = "idx_batch_platform", columnList = "platform_id"),
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_batch_created_at", columnList = "created_at"),
    @Index(name = "idx_batch_imported_by", columnList = "imported_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform_id", nullable = false)
    private Long platformId;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private Integer totalRows = 0;

    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private Integer successCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    private Integer skippedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ImportStatus status = ImportStatus.PROCESSING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "imported_by")
    private Long importedBy;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
