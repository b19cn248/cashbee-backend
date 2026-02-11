package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.ValidateReferralCodeResponse;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use Case: Validate a referral code.
 *
 * This use case checks if a referral code is valid and can be used.
 * Used to provide immediate feedback to users before they submit
 * the referral code.
 *
 * Validation rules:
 * - Code must exist in the system
 * - Code must belong to an active user
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidateReferralCodeUseCase {

    private final UserRepository userRepository;

    /**
     * Execute use case to validate a referral code.
     *
     * @param referralCode The referral code to validate
     * @return Response indicating if code is valid
     */
    @Transactional(readOnly = true)
    public ValidateReferralCodeResponse execute(String referralCode) {
        log.debug("Validating referral code: {}", referralCode);

        // Handle null/empty input
        if (referralCode == null || referralCode.isBlank()) {
            return ValidateReferralCodeResponse.invalid(
                    referralCode,
                    "Referral code is required"
            );
        }

        // Normalize code (uppercase)
        String normalizedCode = referralCode.toUpperCase().trim();

        // Find the referrer
        Optional<User> referrerOpt = userRepository.findByReferralCode(normalizedCode);

        if (referrerOpt.isEmpty()) {
            log.debug("Referral code not found: {}", normalizedCode);
            return ValidateReferralCodeResponse.invalid(
                    normalizedCode,
                    "Referral code not found"
            );
        }

        User referrer = referrerOpt.get();

        // Check if referrer is active
        if (!referrer.isActive()) {
            log.debug("Referrer is inactive: userId={}, status={}",
                    referrer.getId(), referrer.getStatus());
            return ValidateReferralCodeResponse.invalid(
                    normalizedCode,
                    "Referral code belongs to an inactive user"
            );
        }

        // Code is valid
        log.debug("Referral code is valid: code={}, referrerId={}",
                normalizedCode, referrer.getId());

        return ValidateReferralCodeResponse.valid(
                normalizedCode,
                maskName(referrer.getFullName(), referrer.getUsername())
        );
    }

    /**
     * Mask a name for privacy.
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
