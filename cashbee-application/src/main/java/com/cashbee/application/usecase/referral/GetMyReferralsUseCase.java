package com.cashbee.application.usecase.referral;

import com.cashbee.application.dto.referral.GetMyReferralsResponse;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use Case: Get list of users referred by the current user.
 *
 * This use case retrieves all users who entered the current user's
 * referral code when they registered, along with their referral status.
 *
 * Business Logic:
 * - Find all users where referredBy = current user's referralCode
 * - Mask personal information (name, email) for privacy
 * - Calculate active referrals (within 3-month commission period)
 * - Include referral activation status and expiration dates
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetMyReferralsUseCase {

    private final UserRepository userRepository;

    /**
     * Execute the use case.
     *
     * @param keycloakId Keycloak user ID of the current user
     * @return Response containing list of referred users
     */
    @Transactional(readOnly = true)
    public GetMyReferralsResponse execute(String keycloakId) {
        log.debug("Getting referrals for user: keycloakId={}", keycloakId);

        // 1. Find current user
        User currentUser = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> {
                    log.error("User not found: keycloakId={}", keycloakId);
                    return new RuntimeException("User not found: " + keycloakId);
                });

        log.debug("Found user: id={}, referralCode={}", currentUser.getId(), currentUser.getReferralCode());

        // 2. Find all users referred by this user
        List<User> referredUsers = userRepository.findByReferredBy(currentUser.getReferralCode());

        log.debug("Found {} referred users", referredUsers.size());

        // 3. Count active referrals (within 3-month commission period)
        long activeReferrals = referredUsers.stream()
                .filter(User::isWithinReferralPeriod)
                .count();

        // 4. Map to response DTOs with privacy masking
        List<GetMyReferralsResponse.ReferredUserInfo> referralInfos = referredUsers.stream()
                .map(this::mapToReferredUserInfo)
                .toList();

        // 5. Build and return response
        return GetMyReferralsResponse.builder()
                .myReferralCode(currentUser.getReferralCode())
                .totalReferrals(referredUsers.size())
                .activeReferrals((int) activeReferrals)
                .referrals(referralInfos)
                .build();
    }

    /**
     * Map a User domain model to ReferredUserInfo DTO.
     * Applies privacy masking to personal information.
     *
     * @param user The referred user
     * @return DTO with masked information
     */
    private GetMyReferralsResponse.ReferredUserInfo mapToReferredUserInfo(User user) {
        return GetMyReferralsResponse.ReferredUserInfo.builder()
                .name(maskName(user.getFullName(), user.getUsername()))
                .email(maskEmail(user.getEmail()))
                .status(user.getStatus() != null ? user.getStatus().name() : "UNKNOWN")
                .joinedAt(user.getCreatedAt())
                .completedOrders(user.getTotalCompletedOrders() != null ? user.getTotalCompletedOrders() : 0)
                .referralActivated(user.isReferralActivated())
                .referralActivatedAt(user.getReferralActivatedAt())
                .referralExpiresAt(user.getReferralExpiresAt())
                .withinCommissionPeriod(user.isWithinReferralPeriod())
                .build();
    }

    /**
     * Mask a name for privacy.
     * Shows first 2 characters, masks the rest with asterisks.
     *
     * Examples:
     * - "Nguyen Van A" -> "Ng********"
     * - "AB" -> "***" (too short)
     * - null -> "***"
     *
     * @param fullName User's full name
     * @param username Fallback if fullName is null
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

    /**
     * Mask an email for privacy.
     * Shows first 2 characters of local part, masks the rest, keeps domain.
     *
     * Examples:
     * - "nguyen@gmail.com" -> "ng***@gmail.com"
     * - "ab@test.com" -> "ab***@test.com"
     * - null -> "***@***"
     *
     * @param email User's email
     * @return Masked email
     */
    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***@***";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***@***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return localPart + "***" + domain;
        }

        String visible = localPart.substring(0, 2);
        return visible + "***" + domain;
    }
}
