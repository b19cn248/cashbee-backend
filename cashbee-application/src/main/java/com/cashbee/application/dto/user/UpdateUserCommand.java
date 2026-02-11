package com.cashbee.application.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Command DTO for updating user information.
 *
 * Used when user wants to update their profile including:
 * - Basic info (fullName, phone)
 * - Bank account info (accountNumber, bankCode)
 *
 * All fields are optional - only provided fields will be updated.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserCommand {

    /**
     * User's full name.
     * Optional - only update if provided.
     */
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    /**
     * Phone number (Vietnamese format: 0XXXXXXXXX).
     * Optional - only update if provided.
     */
    @Pattern(regexp = "^0\\d{9}$", message = "Phone must be 10 digits starting with 0")
    private String phone;

    /**
     * Bank account number.
     * Optional - only update if provided.
     * If provided, bankCode must also be provided.
     */
    @Size(min = 6, max = 20, message = "Account number must be between 6 and 20 characters")
    private String accountNumber;

    /**
     * Bank account holder name.
     * Optional - defaults to user's full name if not provided.
     */
    @Size(min = 2, max = 100, message = "Account name must be between 2 and 100 characters")
    private String accountName;

    /**
     * Bank code (e.g., VPBANK, ACB).
     * Optional - only update if provided.
     * If provided, accountNumber must also be provided.
     * Must exist in bank table.
     */
    @Size(max = 50, message = "Bank code must not exceed 50 characters")
    private String bankCode;

    /**
     * Mã giới thiệu của người đã giới thiệu user này (optional).
     * Chỉ có thể nhập một lần duy nhất - nếu user đã có referredBy thì không thể thay đổi.
     * Format: CB + 6 ký tự alphanumeric (ví dụ: CB4F7A9K)
     */
    @Pattern(
        regexp = "^CB[A-Z0-9]{6}$",
        message = "Mã giới thiệu phải có định dạng: CB + 6 ký tự chữ và số"
    )
    private String referredBy;

    /**
     * Check if bank account info is being updated.
     *
     * @return true if both accountNumber and bankCode are provided
     */
    public boolean hasBankAccountInfo() {
        return accountNumber != null && !accountNumber.isBlank() &&
               bankCode != null && !bankCode.isBlank();
    }

    /**
     * Validate that if one of accountNumber/bankCode is provided,
     * both must be provided.
     *
     * @throws IllegalArgumentException if only one field is provided
     */
    public void validate() {
        boolean hasAccountNumber = accountNumber != null && !accountNumber.isBlank();
        boolean hasBankCode = bankCode != null && !bankCode.isBlank();

        if (hasAccountNumber && !hasBankCode) {
            throw new IllegalArgumentException(
                "Bank code is required when account number is provided");
        }
        if (hasBankCode && !hasAccountNumber) {
            throw new IllegalArgumentException(
                "Account number is required when bank code is provided");
        }
    }
}
