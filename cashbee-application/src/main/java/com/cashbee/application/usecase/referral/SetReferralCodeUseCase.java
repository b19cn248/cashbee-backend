package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.ReferralValidationResult;
import com.cashbee.application.dto.referral.SetReferralCodeCommand;
import com.cashbee.application.dto.referral.SetReferralCodeResponse;
import com.cashbee.application.service.ReferralCodeValidator;
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
    private final ReferralCodeValidator referralCodeValidator;

    /**
     * Execute use case to set referral code for a user.
     *
     * @param keycloakId Keycloak user ID of the referee (from JWT)
     * @param command Command containing the referral code to set
     * @return Response with referral setup result
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
        referralCodeValidator.checkNotAlreadyReferred(referee);

        // 3. Validate referral code (throws exception if invalid)
        ReferralValidationResult validationResult = referralCodeValidator.validateOrThrow(
                command.getReferralCode(),
                referee.getId()
        );

        // 4. Set the referral code on referee
        referee.setReferredBy(validationResult.getNormalizedCode());

        // 5. Save the referee
        userRepository.save(referee);

        log.info("Referral code set successfully: referee={}, referrer={}, code={}",
                referee.getId(), validationResult.getReferrerId(), validationResult.getNormalizedCode());

        // 6. Build and return response
        return SetReferralCodeResponse.success(
                validationResult.getNormalizedCode(),
                validationResult.getReferrerId(),
                validationResult.getReferrerName()
        );
    }
}
