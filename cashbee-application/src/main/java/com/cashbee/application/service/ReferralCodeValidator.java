package com.cashbee.application.service;

import com.cashbee.application.dto.referral.ReferralValidationResult;
import com.cashbee.common.exception.InvalidReferralCodeException;
import com.cashbee.common.exception.ReferralCodeAlreadySetException;
import com.cashbee.common.exception.SelfReferralException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for validating referral codes.
 *
 * This is the single source of truth for referral code validation logic.
 * Used by:
 * - SetReferralCodeUseCase
 * - SyncUserFromKeycloakUseCase
 * - UpdateUserUseCase
 *
 * Validation rules:
 * 1. Code must not be null or blank
 * 2. Code is normalized to UPPERCASE
 * 3. Code must exist in the system
 * 4. User cannot use their own code (self-referral)
 * 5. Referrer must be active
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralCodeValidator {

    private final UserRepository userRepository;

    /**
     * Validate a referral code without throwing exceptions.
     *
     * Use this method when you want to handle validation result yourself.
     *
     * @param referralCode  The referral code to validate
     * @param currentUserId ID of the current user (for self-referral check), can be null for new users
     * @return ReferralValidationResult containing validation status and details
     */
    public ReferralValidationResult validate(String referralCode, Long currentUserId) {
        log.debug("Validating referral code: code={}, currentUserId={}", referralCode, currentUserId);

        // 1. Check null/blank
        if (referralCode == null || referralCode.isBlank()) {
            log.debug("Referral code is empty");
            return ReferralValidationResult.emptyCode();
        }

        // 2. Normalize code (UPPERCASE + trim)
        String normalizedCode = referralCode.toUpperCase().trim();

        // 3. Find referrer by code
        Optional<User> referrerOpt = userRepository.findByReferralCode(normalizedCode);
        if (referrerOpt.isEmpty()) {
            log.debug("Referral code not found: {}", normalizedCode);
            return ReferralValidationResult.codeNotFound(normalizedCode);
        }

        User referrer = referrerOpt.get();
        log.debug("Found referrer: id={}, username={}", referrer.getId(), referrer.getUsername());

        // 4. Check self-referral (only if currentUserId is provided)
        if (currentUserId != null && currentUserId.equals(referrer.getId())) {
            log.warn("Self-referral attempt: userId={}, code={}", currentUserId, normalizedCode);
            return ReferralValidationResult.selfReferral(normalizedCode, currentUserId);
        }

        // 5. Check referrer is active
        if (!referrer.isActive()) {
            log.debug("Referrer is inactive: id={}, status={}", referrer.getId(), referrer.getStatus());
            return ReferralValidationResult.referrerInactive(normalizedCode, referrer.getId());
        }

        // All validations passed
        String maskedName = maskName(referrer.getFullName(), referrer.getUsername());
        log.debug("Referral code is valid: code={}, referrerId={}", normalizedCode, referrer.getId());

        return ReferralValidationResult.valid(normalizedCode, referrer.getId(), maskedName);
    }

    /**
     * Validate a referral code and throw exception if invalid.
     *
     * Use this method when you want exceptions to be thrown for invalid codes.
     *
     * @param referralCode  The referral code to validate
     * @param currentUserId ID of the current user (for self-referral check), can be null for new users
     * @return ReferralValidationResult (only returned if valid)
     * @throws InvalidReferralCodeException if code is invalid
     * @throws SelfReferralException        if user tries to use own code
     */
    public ReferralValidationResult validateOrThrow(String referralCode, Long currentUserId) {
        ReferralValidationResult result = validate(referralCode, currentUserId);

        if (!result.isValid()) {
            throwExceptionForResult(result, currentUserId);
        }

        return result;
    }

    /**
     * Check if user already has a referral code set and throw if so.
     *
     * @param user The user to check
     * @throws ReferralCodeAlreadySetException if user already has referredBy set
     */
    public void checkNotAlreadyReferred(User user) {
        if (user.hasReferrer()) {
            log.warn("User {} already has referral code set: {}", user.getId(), user.getReferredBy());
            throw ReferralCodeAlreadySetException.forUser(user.getId(), user.getReferredBy());
        }
    }

    /**
     * Throw appropriate exception based on validation result.
     *
     * @param result        The validation result
     * @param currentUserId Current user ID for self-referral exception
     */
    private void throwExceptionForResult(ReferralValidationResult result, Long currentUserId) {
        String errorCode = result.getErrorCode();

        switch (errorCode) {
            case "EMPTY_CODE":
                throw new InvalidReferralCodeException("EMPTY_CODE", result.getErrorMessage());

            case "CODE_NOT_FOUND":
                throw InvalidReferralCodeException.notFound(result.getNormalizedCode());

            case "SELF_REFERRAL":
                throw SelfReferralException.forUser(currentUserId);

            case "REFERRER_INACTIVE":
                throw new InvalidReferralCodeException("REFERRER_INACTIVE", result.getErrorMessage());

            default:
                throw new InvalidReferralCodeException(errorCode, result.getErrorMessage());
        }
    }

    /**
     * Mask a name for privacy.
     * Shows first 2 characters and masks the rest.
     * Example: "Nguyen Van A" -> "Ng********"
     *
     * @param fullName User's full name
     * @param username Fallback username if no full name
     * @return Masked name
     */
    private String maskName(String fullName, String username) {
        String nameToMask = (fullName != null && !fullName.isBlank()) ? fullName : username;

        if (nameToMask == null || nameToMask.length() <= 2) {
            return "***";
        }

        String visible = nameToMask.substring(0, 2);
        int maskLength = Math.min(nameToMask.length() - 2, 8);
        return visible + "*".repeat(maskLength);
    }
}
