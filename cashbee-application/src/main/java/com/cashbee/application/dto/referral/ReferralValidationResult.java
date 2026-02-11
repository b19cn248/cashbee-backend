package com.cashbee.application.dto.referral;

import lombok.*;

/**
 * Result DTO for referral code validation.
 *
 * Contains validation result and related information.
 * Used by ReferralCodeValidator to return validation status
 * without throwing exceptions, allowing callers to decide how to handle.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralValidationResult {

    /**
     * Whether the referral code is valid.
     */
    private boolean valid;

    /**
     * The normalized referral code (UPPERCASE, trimmed).
     * Only set when valid = true.
     */
    private String normalizedCode;

    /**
     * ID of the referrer (user who owns this code).
     * Only set when valid = true.
     */
    private Long referrerId;

    /**
     * Name of the referrer (masked for privacy).
     * Example: "Ng********"
     * Only set when valid = true.
     */
    private String referrerName;

    /**
     * Error code when validation fails.
     * Possible values:
     * - EMPTY_CODE: Code is null or blank
     * - CODE_NOT_FOUND: Code doesn't exist
     * - SELF_REFERRAL: User trying to use own code
     * - REFERRER_INACTIVE: Referrer user is not active
     */
    private String errorCode;

    /**
     * Human-readable error message.
     */
    private String errorMessage;

    // ==================== Factory Methods ====================

    /**
     * Create a valid result.
     *
     * @param normalizedCode The normalized referral code
     * @param referrerId     ID of the referrer
     * @param referrerName   Masked name of the referrer
     * @return Valid result
     */
    public static ReferralValidationResult valid(String normalizedCode, Long referrerId, String referrerName) {
        return ReferralValidationResult.builder()
                .valid(true)
                .normalizedCode(normalizedCode)
                .referrerId(referrerId)
                .referrerName(referrerName)
                .build();
    }

    /**
     * Create an invalid result for empty code.
     *
     * @return Invalid result
     */
    public static ReferralValidationResult emptyCode() {
        return ReferralValidationResult.builder()
                .valid(false)
                .errorCode("EMPTY_CODE")
                .errorMessage("Mã giới thiệu không được để trống")
                .build();
    }

    /**
     * Create an invalid result for code not found.
     *
     * @param code The code that was not found
     * @return Invalid result
     */
    public static ReferralValidationResult codeNotFound(String code) {
        return ReferralValidationResult.builder()
                .valid(false)
                .normalizedCode(code)
                .errorCode("CODE_NOT_FOUND")
                .errorMessage("Mã giới thiệu không tồn tại: " + code)
                .build();
    }

    /**
     * Create an invalid result for self-referral attempt.
     *
     * @param code   The referral code
     * @param userId The user ID attempting self-referral
     * @return Invalid result
     */
    public static ReferralValidationResult selfReferral(String code, Long userId) {
        return ReferralValidationResult.builder()
                .valid(false)
                .normalizedCode(code)
                .errorCode("SELF_REFERRAL")
                .errorMessage("Không thể sử dụng mã giới thiệu của chính bạn")
                .build();
    }

    /**
     * Create an invalid result for inactive referrer.
     *
     * @param code       The referral code
     * @param referrerId The inactive referrer's ID
     * @return Invalid result
     */
    public static ReferralValidationResult referrerInactive(String code, Long referrerId) {
        return ReferralValidationResult.builder()
                .valid(false)
                .normalizedCode(code)
                .referrerId(referrerId)
                .errorCode("REFERRER_INACTIVE")
                .errorMessage("Mã giới thiệu không còn hoạt động")
                .build();
    }
}
