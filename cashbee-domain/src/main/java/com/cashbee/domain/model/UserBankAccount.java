package com.cashbee.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * UserBankAccount Domain Model.
 *
 * Thông tin ngân hàng mặc định của user.
 * User phải điền thông tin này trước khi rút tiền.
 *
 * Business Rules:
 * - Mỗi user chỉ có 1 tài khoản ngân hàng mặc định
 * - Account number, account name, bank name là bắt buộc
 * - Thông tin này được sử dụng khi tạo file chuyển khoản lô
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"accountNumber"})
@EqualsAndHashCode(of = {"id"})
public class UserBankAccount {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * User ID (foreign key to user table).
     * Mỗi user chỉ có 1 bank account.
     */
    private Long userId;

    /**
     * Số tài khoản ngân hàng.
     * Required, max 50 ký tự.
     */
    private String accountNumber;

    /**
     * Tên chủ tài khoản (theo đúng CMND/CCCD).
     * Required, max 200 ký tự.
     */
    private String accountName;

    /**
     * Tên ngân hàng (VD: "Ngân hàng TMCP Việt Nam Thịnh Vượng").
     * Required, max 100 ký tự.
     */
    private String bankName;

    /**
     * Mã ngân hàng (VD: "VPBANK", "VCB", "ACB").
     * Optional, max 20 ký tự.
     */
    private String bankCode;

    /**
     * Có phải là tài khoản mặc định không.
     * Default: true
     */
    @Builder.Default
    private Boolean isDefault = true;

    /**
     * Tài khoản đã được xác minh chưa (KYC).
     * Default: false
     */
    @Builder.Default
    private Boolean verified = false;

    /**
     * Timestamp when entity was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when entity was last updated.
     */
    private LocalDateTime updatedAt;

    // ===== Business Logic Methods =====

    /**
     * Validate business rules.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (userId == null) {
            throw new IllegalStateException("User ID is required");
        }

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalStateException("Account number is required");
        }

        if (accountNumber.length() > 50) {
            throw new IllegalStateException("Account number too long (max 50 characters)");
        }

        if (accountName == null || accountName.isBlank()) {
            throw new IllegalStateException("Account name is required");
        }

        if (accountName.length() > 200) {
            throw new IllegalStateException("Account name too long (max 200 characters)");
        }

        if (bankName == null || bankName.isBlank()) {
            throw new IllegalStateException("Bank name is required");
        }

        if (bankName.length() > 100) {
            throw new IllegalStateException("Bank name too long (max 100 characters)");
        }

        if (bankCode != null && bankCode.length() > 20) {
            throw new IllegalStateException("Bank code too long (max 20 characters)");
        }
    }

    /**
     * Check if this is a new bank account (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Mark account as verified.
     */
    public void markAsVerified() {
        this.verified = true;
    }

    /**
     * Mark account as unverified.
     */
    public void markAsUnverified() {
        this.verified = false;
    }
}
