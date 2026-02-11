package com.cashbee.application.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing one row in the Excel batch transfer file.
 *
 * <p>Maps to columns in VPBank TTTN (Thanh Toan Trong Nuoc) template:
 * <ul>
 *   <li>STT - Row number</li>
 *   <li>Account - Bank account number</li>
 *   <li>Currency - Currency code (VND, USD, EUR...)</li>
 *   <li>Ben_Name - Beneficiary name (no Vietnamese, no special chars)</li>
 *   <li>Bank_Code - VPBank bank code (9 digits, empty for internal VPBank)</li>
 *   <li>Bank_Name - Bank short name (e.g., VIETCOMBANK, VPBANK)</li>
 *   <li>Branch_Name - Branch name (empty for internal VPBank)</li>
 *   <li>City_Name - City name (empty for internal VPBank)</li>
 *   <li>Amount - Transfer amount</li>
 *   <li>Details - Payment details (no Vietnamese)</li>
 *   <li>Charges - Fee type: OUR or BEN</li>
 * </ul>
 *
 * <p>For VietinBank template, uses vietinbankCode instead of vpbankCode.
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTransferRow {

    /**
     * STT - Row number (auto-incremented: 1, 2, 3...).
     */
    private Integer stt;

    /**
     * Account - Bank account number.
     * Only alphanumeric characters allowed.
     */
    private String accountNumber;

    /**
     * Ben_Name - Beneficiary account name.
     * Will be converted to uppercase, no Vietnamese accents.
     */
    private String accountName;

    /**
     * Amount - Transfer amount.
     * For VND: integer only (no decimal point).
     * For other currencies: max 2 decimal places.
     */
    private BigDecimal amount;

    /**
     * Currency - Currency code.
     * Supported: VND, USD, EUR, GBP, CAD, AUD, JPY, CHF, SGD.
     * Default: VND.
     */
    @Builder.Default
    private String currency = "VND";

    /**
     * Bank_Name - Bank short name (e.g., VIETCOMBANK, TECHCOMBANK).
     * For internal VPBank transfer, must contain "VPBank".
     */
    private String bankName;

    /**
     * Bank_Code - Internal bank code used in system (e.g., VPBANK, ACB).
     * This is NOT the VPBank 9-digit code.
     */
    private String bankCode;

    /**
     * VPBank Bank_Code - 9-digit bank code for VPBank format.
     * Example: "101203001" (Vietcombank), "101310001" (Techcombank).
     * Empty/null for internal VPBank transfers.
     */
    private String vpbankCode;

    /**
     * Branch_Name - Bank branch name.
     * Not required for internal VPBank transfers.
     * No Vietnamese, no special characters.
     */
    @Builder.Default
    private String branchName = "";

    /**
     * City_Name - City/Province name.
     * Not required for internal VPBank transfers.
     * No Vietnamese (e.g., "Ha Noi" not "Hà Nội").
     */
    @Builder.Default
    private String cityName = "";

    /**
     * Details - Payment description/remark.
     * No Vietnamese, no special characters.
     * Allowed: SPACE A-Za-z0-9.+-)(,
     */
    private String remark;

    /**
     * Charges - Fee type.
     * OUR: Sender pays all fees.
     * BEN: Beneficiary pays fees.
     * For internal VPBank transfers, always treated as OUR.
     */
    @Builder.Default
    private String charges = "OUR";

    /**
     * User ID - Internal reference (not exported to Excel).
     * Used for logging/debugging only.
     */
    private Long userId;

    /**
     * VietinBank code - 8-digit bank code for VietinBank format.
     * Example: "01309001" (VPBank), "01202001" (BIDV).
     * Special: "VietinBank" for internal VietinBank transfer.
     * Used only for VietinBank template.
     */
    private String vietinbankCode;

    /**
     * VPBank ID - Internal VPBank bank ID (integer).
     * Legacy field, may be deprecated.
     */
    private Integer vpbankId;

    /**
     * Check if this is an internal VPBank transfer.
     * Internal transfers have empty Bank_Code, Branch_Name, City_Name.
     *
     * @return true if transferring to VPBank account
     */
    public boolean isInternalVPBankTransfer() {
        return bankCode != null && bankCode.equalsIgnoreCase("VPBANK");
    }
}
