package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.CreateMilestoneRequest;
import com.cashbee.application.dto.referraladmin.MilestoneConfigResponse;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.repository.MilestoneConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Use case for creating a new milestone configuration.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateMilestoneUseCase {

    private final MilestoneConfigRepository repository;

    /**
     * Create a new milestone configuration.
     *
     * @param request create request
     * @return created milestone response
     * @throws BusinessException if validation fails or duplicate exists
     */
    @Transactional
    public MilestoneConfigResponse execute(CreateMilestoneRequest request) {
        log.info("UseCase: Creating milestone - type: {}, ordersRequired: {}",
                request.getMilestoneType(), request.getOrdersRequired());

        // Check for duplicate
        if (repository.existsByMilestoneTypeAndOrdersRequired(
                request.getMilestoneType(), request.getOrdersRequired())) {
            throw new BusinessException(
                    "DUPLICATE_MILESTONE",
                    "A milestone already exists for type " + request.getMilestoneType()
                            + " with " + request.getOrdersRequired() + " orders required"
            );
        }

        // Build domain model
        MilestoneConfig config = MilestoneConfig.builder()
                .milestoneType(request.getMilestoneType())
                .ordersRequired(request.getOrdersRequired())
                .refereeBonus(request.getRefereeBonus())
                .referrerBonus(request.getReferrerBonus())
                .newTier(request.getNewTier())
                .commissionMonths(request.getCommissionMonths() != null ? request.getCommissionMonths() : 0)
                .activatesReferral(request.getActivatesReferral() != null ? request.getActivatesReferral() : false)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Validate
        try {
            config.validate();
        } catch (IllegalStateException e) {
            throw new BusinessException("VALIDATION_FAILED", e.getMessage());
        }

        // Save
        MilestoneConfig saved = repository.save(config);

        log.info("UseCase: Created milestone with ID: {}", saved.getId());

        return buildResponse(saved);
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
