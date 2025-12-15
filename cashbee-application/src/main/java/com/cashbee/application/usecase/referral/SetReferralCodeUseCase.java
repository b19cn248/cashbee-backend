package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.SetReferralCodeCommand;
import com.cashbee.application.dto.referral.SetReferralCodeResponse;
import com.cashbee.common.exception.InvalidReferralCodeException;
import com.cashbee.common.exception.ReferralCodeAlreadySetException;
import com.cashbee.common.exception.SelfReferralException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Set referral code for a user (making them a referee).
 *
 * This use case handles the scenario when a user enters a referral code
 * from another user (the referrer). Can be invoked during:
 * - User registration
 * - When user adds bank account
 *
 * Business rules:
 * - User can only set referral code once
 * - User cannot use their own referral code (self-referral)
 * - Referral code must exist and belong to an active user
 * - Code matching is case-insensitive
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SetReferralCodeUseCase {

    private final UserRepository userRepository;

    /**
     * Execute use case to set referral code for a user.
     *
     * @param keycloakId Keycloak user ID of the referee (from JWT)
     * @param command Command containing the referral code to set
     * @return Response with referral setup result
     * @throws InvalidReferralCodeException if referral code is invalid or not found
     * @throws ReferralCodeAlreadySetException if user already has a referral code
     * @throws SelfReferralException if user tries to use their own code
     */
    @Transactional
    public SetReferralCodeResponse execute(String keycloakId, SetReferralCodeCommand command) {
        log.info("Setting referral code for user: keycloakId={}, referralCode={}",
                keycloakId, command.getReferralCode());

        // 1. Find the referee (user who is being referred)
        User referee = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> {
                    log.error("User not found: keycloakId={}", keycloakId);
                    return new RuntimeException("User not found: " + keycloakId);
                });

        log.debug("Found referee: userId={}, username={}", referee.getId(), referee.getUsername());

        // 2. Check if user already has referral code set
        if (referee.hasReferrer()) {
            log.warn("User {} already has referral code set: {}",
                    referee.getId(), referee.getReferredBy());
            throw ReferralCodeAlreadySetException.forUser(referee.getId(), referee.getReferredBy());
        }

        // 3. Normalize referral code (uppercase)
        String normalizedCode = command.getReferralCode().toUpperCase().trim();

        // 4. Find the referrer by referral code
        User referrer = userRepository.findByReferralCode(normalizedCode)
                .orElseThrow(() -> {
                    log.warn("Referral code not found: {}", normalizedCode);
                    return InvalidReferralCodeException.notFound(normalizedCode);
                });

        log.debug("Found referrer: userId={}, username={}", referrer.getId(), referrer.getUsername());

        // 5. Check for self-referral
        if (referee.getId().equals(referrer.getId())) {
            log.warn("Self-referral attempt by user: {}", referee.getId());
            throw SelfReferralException.forUser(referee.getId());
        }

        // 6. Check if referrer is active
        if (!referrer.isActive()) {
            log.warn("Referrer {} is inactive (status={})",
                    referrer.getId(), referrer.getStatus());
            throw new InvalidReferralCodeException(
                    "REFERRER_INACTIVE",
                    String.format("Referral code belongs to an inactive user: %s", normalizedCode)
            );
        }

        // 7. Set the referral code on referee
        referee.setReferredBy(normalizedCode);

        // 8. Save the referee
        userRepository.save(referee);

        log.info("Referral code set successfully: referee={}, referrer={}, code={}",
                referee.getId(), referrer.getId(), normalizedCode);

        // 9. Build and return response
        return SetReferralCodeResponse.success(
                normalizedCode,
                referrer.getId(),
                maskName(referrer.getFullName(), referrer.getUsername())
        );
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
