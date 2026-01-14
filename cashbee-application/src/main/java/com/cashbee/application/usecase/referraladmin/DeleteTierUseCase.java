package com.cashbee.application.usecase.referraladmin;

import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.repository.ReferrerTierConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for deleting a referrer tier configuration.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteTierUseCase {

    private final ReferrerTierConfigRepository repository;

    /**
     * Delete a referrer tier configuration by ID.
     *
     * @param id tier config ID
     * @throws BusinessException if not found
     */
    @Transactional
    public void execute(Long id) {
        log.info("UseCase: Deleting tier ID: {}", id);

        // Check if exists
        if (repository.findById(id).isEmpty()) {
            throw new BusinessException(
                    "TIER_NOT_FOUND",
                    "Referrer tier configuration not found with ID: " + id
            );
        }

        // Delete
        repository.deleteById(id);

        log.info("UseCase: Deleted tier ID: {}", id);
    }
}
