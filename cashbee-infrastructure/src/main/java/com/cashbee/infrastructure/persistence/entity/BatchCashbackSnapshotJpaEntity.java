package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for batch_cashback_snapshot table.
 *
 * Snapshot of which cashbacks are included in each batch at creation time.
 *
 * Table: batch_cashback_snapshot
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "batch_cashback_snapshot",
        indexes = {
                @Index(name = "idx_batch_cashback_snapshot_batch", columnList = "batch_id"),
                @Index(name = "idx_batch_cashback_snapshot_user", columnList = "user_id"),
                @Index(name = "idx_batch_cashback_snapshot_batch_user", columnList = "batch_id, user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_batch_cashback_snapshot", columnNames = {"batch_id", "cashback_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BatchCashbackSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to batch_transfer_export.
     */
    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    /**
     * Reference to cashback.
     */
    @Column(name = "cashback_id", nullable = false)
    private Long cashbackId;

    /**
     * User ID for easier querying (denormalized).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Amount at snapshot time (for audit trail).
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Timestamp when snapshot was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
