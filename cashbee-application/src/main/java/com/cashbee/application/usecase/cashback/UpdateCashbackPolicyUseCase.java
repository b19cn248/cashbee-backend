package com.cashbee.application.usecase.cashback;

import com.cashbee.application.dto.cashback.CashbackPolicyResponse;
import com.cashbee.application.dto.cashback.UpdateCashbackPolicyRequest;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.model.CashbackPolicy;
import com.cashbee.domain.repository.CashbackPolicyRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for updating a cashback policy.
 * <p>
 * This use case handles: 1. Finding the existing policy by ID 2. Validating update request 3.
 * Applying partial updates (only non-null fields) 4. Validating business rules 5. Saving and
 * returning updated policy
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateCashbackPolicyUseCase {

  private final CashbackPolicyRepository policyRepository;

  /**
   * Update a cashback policy by ID.
   *
   * @param id      policy ID to update
   * @param request update request with new values
   * @return updated policy response
   * @throws BusinessException if policy not found or validation fails
   */
  @Transactional
  public CashbackPolicyResponse execute(Long id, UpdateCashbackPolicyRequest request) {
    log.info("UseCase: Updating cashback policy ID: {}", id);

    // 1. Validate request has something to update
    if (!request.hasAnyFieldToUpdate()) {
      throw new BusinessException("INVALID_REQUEST", "No fields provided for update");
    }

    // 2. Find existing policy
    CashbackPolicy policy = policyRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "POLICY_NOT_FOUND",
            "Cashback policy not found with ID: " + id
        ));

    log.debug("UseCase: Found policy: {} ({})", policy.getPolicyName(), policy.getPolicyCode());

    // 3. Apply partial updates (only non-null fields)
    applyUpdates(policy, request);

    // 4. Update timestamp
    policy.setUpdatedAt(LocalDateTime.now());

    // 5. Validate business rules
    try {
      policy.validate();
    } catch (IllegalStateException e) {
      throw new BusinessException("VALIDATION_FAILED", e.getMessage());
    }

    // 6. Validate date range if both dates provided
    if (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
        && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
      throw new BusinessException(
          "INVALID_DATE_RANGE",
          "Effective end date must be after start date"
      );
    }

    // 7. Save updated policy
    CashbackPolicy savedPolicy = policyRepository.save(policy);

    log.info("UseCase: Successfully updated policy ID: {} ({})",
        savedPolicy.getId(), savedPolicy.getPolicyName());

    // 8. Build and return response
    return buildResponse(savedPolicy);
  }

  /**
   * Apply updates from request to policy. Only non-null fields in request will be applied.
   *
   * @param policy  existing policy to update
   * @param request update request with new values
   */
  private void applyUpdates(CashbackPolicy policy, UpdateCashbackPolicyRequest request) {
    if (request.getCashbackRate() != null) {
      log.debug("Updating cashbackRate: {} -> {}",
          policy.getCashbackRate(), request.getCashbackRate());
      policy.setCashbackRate(request.getCashbackRate());
    }

    if (request.getMinOrderValue() != null) {
      log.debug("Updating minOrderValue: {} -> {}",
          policy.getMinOrderValue(), request.getMinOrderValue());
      policy.setMinOrderValue(request.getMinOrderValue());
    }

    if (request.getMaxCashbackPerOrder() != null) {
      log.debug("Updating maxCashbackPerOrder: {} -> {}",
          policy.getMaxCashbackPerOrder(), request.getMaxCashbackPerOrder());
      policy.setMaxCashbackPerOrder(request.getMaxCashbackPerOrder());
    }

    if (request.getIsActive() != null) {
      log.debug("Updating isActive: {} -> {}",
          policy.getIsActive(), request.getIsActive());
      policy.setIsActive(request.getIsActive());
    }

    if (request.getPriority() != null) {
      log.debug("Updating priority: {} -> {}",
          policy.getPriority(), request.getPriority());
      policy.setPriority(request.getPriority());
    }

    if (request.getEffectiveFrom() != null) {
      log.debug("Updating effectiveFrom: {} -> {}",
          policy.getEffectiveFrom(), request.getEffectiveFrom());
      policy.setEffectiveFrom(request.getEffectiveFrom());
    }

    if (request.getEffectiveTo() != null) {
      log.debug("Updating effectiveTo: {} -> {}",
          policy.getEffectiveTo(), request.getEffectiveTo());
      policy.setEffectiveTo(request.getEffectiveTo());
    }

    if (request.getPolicyName() != null) {
      log.debug("Updating policyName: {} -> {}",
          policy.getPolicyName(), request.getPolicyName());
      policy.setPolicyName(request.getPolicyName());
    }

    if (request.getPlatformId() != null) {
      log.debug("Updating platformId: {} -> {}",
          policy.getPlatformId(), request.getPlatformId());
      policy.setPlatformId(request.getPlatformId());
    }
  }

  /**
   * Build response DTO from domain model.
   *
   * @param policy domain model
   * @return response DTO
   */
  private CashbackPolicyResponse buildResponse(CashbackPolicy policy) {
    return CashbackPolicyResponse.builder()
        .id(policy.getId())
        .policyName(policy.getPolicyName())
        .policyCode(policy.getPolicyCode())
        .platformId(policy.getPlatformId())
        .userLevel(policy.getUserLevel() != null ? policy.getUserLevel().name() : null)
        .cashbackRate(policy.getCashbackRate())
        .minOrderValue(policy.getMinOrderValue())
        .maxCashbackPerOrder(policy.getMaxCashbackPerOrder())
        .isActive(policy.getIsActive())
        .priority(policy.getPriority())
        .effectiveFrom(policy.getEffectiveFrom())
        .effectiveTo(policy.getEffectiveTo())
        .build();
  }

  /**
   * Get a single policy by ID.
   *
   * @param id policy ID
   * @return policy response
   * @throws BusinessException if policy not found
   */
  @Transactional(readOnly = true)
  public CashbackPolicyResponse getById(Long id) {
    log.info("UseCase: Getting cashback policy by ID: {}", id);

    CashbackPolicy policy = policyRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "POLICY_NOT_FOUND",
            "Cashback policy not found with ID: " + id
        ));

    return buildResponse(policy);
  }
}
