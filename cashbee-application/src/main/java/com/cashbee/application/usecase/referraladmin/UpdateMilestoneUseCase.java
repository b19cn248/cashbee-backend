package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.MilestoneConfigResponse;
import com.cashbee.application.dto.referraladmin.UpdateMilestoneRequest;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.repository.MilestoneConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for updating a milestone configuration.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateMilestoneUseCase {

    private final MilestoneConfigRepository repository;

    /**
     * Update an existing milestone configuration.
     *
     * @param id milestone config ID
     * @param request update request
     * @return updated milestone response
     * @throws BusinessException if not found or validation fails
     */
    @Transactional
    public MilestoneConfigResponse execute(Long id, UpdateMilestoneRequest request) {
        log.info("UseCase: Updating milestone ID: {}", id);

        // Validate request has something to update
        if (!request.hasAnyFieldToUpdate()) {
            throw new BusinessException("INVALID_REQUEST", "No fields provided for update");
        }

        // Find existing
        MilestoneConfig config = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "MILESTONE_NOT_FOUND",
                        "Milestone configuration not found with ID: " + id
                ));

        // Apply updates
        applyUpdates(config, request);

        // Update timestamp
        config.setUpdatedAt(LocalDateTime.now());

        // Validate
        try {
            config.validate();
        } catch (IllegalStateException e) {
            throw new BusinessException("VALIDATION_FAILED", e.getMessage());
        }

        // Check for duplicate if type or ordersRequired changed
        if (request.getMilestoneType() != null || request.getOrdersRequired() != null) {
            var existing = repository.findByMilestoneTypeAndOrdersRequired(
                    config.getMilestoneType(), config.getOrdersRequired());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new BusinessException(
                        "DUPLICATE_MILESTONE",
                        "A milestone already exists for type " + config.getMilestoneType()
                                + " with " + config.getOrdersRequired() + " orders required"
                );
            }
        }

        // Save
        MilestoneConfig saved = repository.save(config);

        log.info("UseCase: Updated milestone ID: {}", saved.getId());

        return buildResponse(saved);
    }

    private void applyUpdates(MilestoneConfig config, UpdateMilestoneRequest request) {
        if (request.getMilestoneType() != null) {
            config.setMilestoneType(request.getMilestoneType());
        }
        if (request.getOrdersRequired() != null) {
            config.setOrdersRequired(request.getOrdersRequired());
        }
        if (request.getRefereeBonus() != null) {
            config.setRefereeBonus(request.getRefereeBonus());
        }
        if (request.getReferrerBonus() != null) {
            config.setReferrerBonus(request.getReferrerBonus());
        }
        if (request.getNewTier() != null) {
            config.setNewTier(request.getNewTier());
        }
        if (request.getCommissionMonths() != null) {
            config.setCommissionMonths(request.getCommissionMonths());
        }
        if (request.getActivatesReferral() != null) {
            config.setActivatesReferral(request.getActivatesReferral());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
    }

    private MilestoneConfigResponse buildResponse(MilestoneConfig config) {
        return MilestoneConfigResponse.builder()
                .id(config.getId())
                .milestoneType(config.getMilestoneType() != null ? config.getMilestoneType().name() : null)
                .ordersRequired(config.getOrdersRequired())
                .refereeBonus(config.getRefereeBonus())
                .referrerBonus(config.getReferrerBonus())
                .newTier(config.getNewTier() != null ? config.getNewTier().name() : null)
                .commissionMonths(config.getCommissionMonths())
                .activatesReferral(config.getActivatesReferral())
                .description(config.getDescription())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
