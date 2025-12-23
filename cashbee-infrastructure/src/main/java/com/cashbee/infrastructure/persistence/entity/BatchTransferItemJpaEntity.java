package com.cashbee.infrastructure.persistence.entity;

import com.cashbee.domain.enums.BatchItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity for batch_transfer_item table.
 *
 * Lưu snapshot chi tiết từng user trong một batch transfer.
 *
 * Table: batch_transfer_item
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "batch_transfer_item", indexes = {
        @Index(name = "idx_batch_transfer_item_batch_id", columnList = "batch_id"),
        @Index(name = "idx_batch_transfer_item_user_id", columnList = "user_id"),
        @Index(name = "idx_batch_transfer_item_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BatchTransferItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to batch_transfer_export.
     */
    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    /**
     * User ID.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Wallet ID.
     */
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    /**
     * Snapshot amount tại thời điểm tạo batch.
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Snapshot account number.
     */
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    /**
     * Snapshot account name.
     */
    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    /**
     * Snapshot bank name.
     */
    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    /**
     * Trạng thái: PENDING -> COMPLETED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BatchItemStatus status = BatchItemStatus.PENDING;

    /**
     * Timestamp khi tạo item.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp khi hoàn thành thanh toán.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Error message if processing failed.
     * Null if successful or not yet processed.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Actual amount deducted (may differ from snapshot if balance changed).
     */
    @Column(name = "actual_amount_deducted", precision = 15, scale = 2)
    private BigDecimal actualAmountDeducted;
}
