package com.cashbee.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Bank Domain Model.
 * Represents a supported bank in the CashBee system.
 *
 * This is used for:
 * - Validating bank codes when users add/update bank account information
 * - Displaying list of available banks in the UI
 * - Ensuring data consistency (no typos in bank codes)
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class Bank {

    /**
     * Internal database ID (auto-generated).
     */
    private Long id;

    /**
     * Unique bank code.
     * Examples: VPBANK, ACB, TECHCOMBANK
     * Used for integration with banking APIs and validation.
     */
    private String bankCode;

    /**
     * Full bank name (Vietnamese).
     * Examples: "Ngân hàng TMCP Việt Nam Thịnh Vượng (VPBank)"
     */
    private String bankName;

    /**
     * Short name for display.
     * Examples: "VPBank", "ACB", "Techcombank"
     * Used in UI dropdowns for better readability.
     */
    private String shortName;

    /**
     * Whether this bank is currently active/supported.
     * Can be used to temporarily disable banks without deleting them.
     */
    @Builder.Default
    private Boolean isActive = true;

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
     * Check if bank is active.
     *
     * @return true if bank is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }

    /**
     * Activate this bank.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivate this bank.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Check if this is a new bank (not persisted yet).
     *
     * @return true if id is null
     */
    public boolean isNew() {
        return this.id == null;
    }

    /**
     * Validate bank data.
     * Throws exception if validation fails.
     */
    public void validate() {
        if (this.bankCode == null || this.bankCode.isBlank()) {
            throw new IllegalStateException("Bank code is required");
        }
        if (this.bankName == null || this.bankName.isBlank()) {
            throw new IllegalStateException("Bank name is required");
        }
        if (this.shortName == null || this.shortName.isBlank()) {
            throw new IllegalStateException("Short name is required");
        }
        if (this.isActive == null) {
            throw new IllegalStateException("Active status is required");
        }
    }
}
