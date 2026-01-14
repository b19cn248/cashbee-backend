package com.cashbee.application.usecase.referraladmin;

import com.cashbee.application.dto.referraladmin.MilestoneConfigResponse;
import com.cashbee.domain.enums.MilestoneType;
import com.cashbee.domain.model.MilestoneConfig;
import com.cashbee.domain.repository.MilestoneConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving all milestone configurations.
 * Supports filtering by milestone type and active status.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAllMilestonesUseCase {

    private final MilestoneConfigRepository repository;

    /**
     * Get all milestone configurations with optional filters.
     *
     * @param milestoneType optional filter by milestone type
     * @param isActive optional filter by active status
     * @return list of milestone config responses
     */
    @Transactional(readOnly = true)
    public List<MilestoneConfigResponse> execute(MilestoneType milestoneType, Boolean isActive) {
        log.info("UseCase: Getting all milestones with filters - type: {}, isActive: {}",
                milestoneType, isActive);

        List<MilestoneConfig> configs;

        if (milestoneType != null && Boolean.TRUE.equals(isActive)) {
            configs = repository.findActiveByMilestoneType(milestoneType);
        } else if (milestoneType != null) {
            configs = repository.findByMilestoneType(milestoneType);
        } else if (Boolean.TRUE.equals(isActive)) {
            configs = repository.findAllActive();
        } else {
            configs = repository.findAll();
        }

        log.info("UseCase: Found {} milestones", configs.size());

        return configs.stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
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
