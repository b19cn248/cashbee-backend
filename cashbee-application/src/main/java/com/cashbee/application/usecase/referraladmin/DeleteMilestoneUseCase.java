package com.cashbee.application.usecase.referraladmin;

import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.repository.MilestoneConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for deleting a milestone configuration.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteMilestoneUseCase {

    private final MilestoneConfigRepository repository;

    /**
     * Delete a milestone configuration by ID.
     *
     * @param id milestone config ID
     * @throws BusinessException if not found
     */
    @Transactional
    public void execute(Long id) {
        log.info("UseCase: Deleting milestone ID: {}", id);

        // Check if exists
        if (repository.findById(id).isEmpty()) {
            throw new BusinessException(
                    "MILESTONE_NOT_FOUND",
                    "Milestone configuration not found with ID: " + id
            );
        }

        // Delete
        repository.deleteById(id);

        log.info("UseCase: Deleted milestone ID: {}", id);
    }
}
