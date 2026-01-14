package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.ReferrerTierConfigResponse;
import com.cashbee.application.dto.referraladmin.UpdateTierRequest;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.ReferrerTierConfig;
import com.cashbee.domain.repository.ReferrerTierConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for updating a referrer tier configuration.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTierUseCase {

    private final ReferrerTierConfigRepository repository;

    /**
     * Update an existing referrer tier configuration.
     *
     * @param id tier config ID
     * @param request update request
     * @return updated tier response
     * @throws BusinessException if not found or validation fails
     */
    @Transactional
    public ReferrerTierConfigResponse execute(Long id, UpdateTierRequest request) {
        log.info("UseCase: Updating tier ID: {}", id);

        // Validate request has something to update
        if (!request.hasAnyFieldToUpdate()) {
            throw new BusinessException("INVALID_REQUEST", "No fields provided for update");
        }

        // Find existing
        ReferrerTierConfig config = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "TIER_NOT_FOUND",
                        "Referrer tier configuration not found with ID: " + id
                ));

        // Check for duplicate tier name if changed
        if (request.getTierName() != null && !request.getTierName().equals(config.getTierName())) {
            var existing = repository.findByTierName(request.getTierName());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new BusinessException(
                        "DUPLICATE_TIER",
                        "A tier already exists with name: " + request.getTierName()
                );
            }
        }

        // Apply updates
        applyUpdates(config, request);

        // Update timestamp
        config.setUpdatedAt(LocalDateTime.now());

        // Save
        ReferrerTierConfig saved = repository.save(config);

        log.info("UseCase: Updated tier ID: {}", saved.getId());

        return buildResponse(saved);
    }

    private void applyUpdates(ReferrerTierConfig config, UpdateTierRequest request) {
        if (request.getTierName() != null) {
            config.setTierName(request.getTierName());
        }
        if (request.getMinReferrals() != null) {
            config.setMinReferrals(request.getMinReferrals());
        }
        if (request.getCommissionRate() != null) {
            config.setCommissionRate(request.getCommissionRate());
        }
        if (request.getBonusPerActivation() != null) {
            config.setBonusPerActivation(request.getBonusPerActivation());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
    }

    private ReferrerTierConfigResponse buildResponse(ReferrerTierConfig config) {
        return ReferrerTierConfigResponse.builder()
                .id(config.getId())
                .tierName(config.getTierName())
                .minReferrals(config.getMinReferrals())
                .commissionRate(config.getCommissionRate())
                .bonusPerActivation(config.getBonusPerActivation())
                .description(config.getDescription())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
