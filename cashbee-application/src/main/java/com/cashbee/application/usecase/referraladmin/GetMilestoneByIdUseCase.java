package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.MilestoneConfigResponse;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.repository.MilestoneConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving a milestone configuration by ID.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetMilestoneByIdUseCase {

    private final MilestoneConfigRepository repository;

    /**
     * Get a milestone configuration by ID.
     *
     * @param id milestone config ID
     * @return milestone config response
     * @throws BusinessException if not found
     */
    @Transactional(readOnly = true)
    public MilestoneConfigResponse execute(Long id) {
        log.info("UseCase: Getting milestone by ID: {}", id);

        MilestoneConfig config = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "MILESTONE_NOT_FOUND",
                        "Milestone configuration not found with ID: " + id
                ));

        log.info("UseCase: Found milestone - type: {}, ordersRequired: {}",
                config.getMilestoneType(), config.getOrdersRequired());

        return buildResponse(config);
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
