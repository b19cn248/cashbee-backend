package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity for user_bank_account table.
 *
 * This entity lives in the infrastructure layer and is the database representation
 * of the UserBankAccount domain model.
 *
 * Table: user_bank_account
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "user_bank_account", indexes = {
        @Index(name = "idx_user_bank_account_user_id", columnList = "user_id"),
        @Index(name = "idx_user_bank_account_bank_code", columnList = "bank_code"),
        @Index(name = "idx_user_bank_account_verified", columnList = "verified")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"accountNumber"})
public class UserBankAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID (FK to user table).
     * UNIQUE - each user has only one bank account.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * Số tài khoản ngân hàng.
     */
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    /**
     * Tên chủ tài khoản.
     */
    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    /**
     * Tên ngân hàng.
     */
    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    /**
     * Mã ngân hàng (VD: VPBANK, VCB).
     */
    @Column(name = "bank_code", length = 20)
    private String bankCode;

    /**
     * Có phải tài khoản mặc định không.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = true;

    /**
     * Tài khoản đã được xác minh chưa.
     */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    /**
     * Timestamp when entity was created (auto-populated).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when entity was last updated (auto-populated).
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
