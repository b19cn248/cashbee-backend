package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.payout.ApprovePayoutRequestCommand;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.application.port.PayoutRequestMapper;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.repository.PayoutRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for approving payout request.
 *
 * Business Scenario:
 * Admin reviews payout request and approves it for processing.
 * Balance is already locked (done during creation).
 * This just changes status and records approval.
 *
 * Flow:
 * 1. Find payout request by ID
 * 2. Validate status is REQUESTED
 * 3. Approve payout (status → PROCESSING)
 * 4. Save updated payout request
 * 5. Return response
 *
 * Next step: System processes payout and transfers money.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovePayoutRequestUseCase {

    private final PayoutRequestRepository payoutRequestRepository;
    private final PayoutRequestMapper payoutRequestMapper;

    /**
     * Execute approve payout request operation.
     *
     * @param command Approve payout request command
     * @return Updated payout request response
     * @throws NullPointerException if command is null
     * @throws NotFoundException if payout request not found
     * @throws IllegalStateException if status is not REQUESTED
     */
    @Transactional
    public PayoutRequestResponse execute(ApprovePayoutRequestCommand command) {
        log.info("Approving payout request: payoutRequestId={}, adminId={}",
                command.getPayoutRequestId(), command.getAdminId());

        // Step 1: Validate command (null check)
        if (command == null) {
            throw new NullPointerException("ApprovePayoutRequestCommand cannot be null");
        }

        // Step 2: Find payout request by ID
        PayoutRequest payoutRequest = payoutRequestRepository.findById(command.getPayoutRequestId())
                .orElseThrow(() -> NotFoundException.of(
                        "PAYOUT_NOT_FOUND",
                        "Payout request not found with ID: " + command.getPayoutRequestId()
                ));

        // Step 3: Approve payout (domain method handles state validation)
        payoutRequest.approve(command.getAdminId());

        // Step 4: Save updated payout request
        PayoutRequest savedPayoutRequest = payoutRequestRepository.save(payoutRequest);

        log.info("Payout request approved successfully: id={}, userId={}, adminId={}",
                savedPayoutRequest.getId(),
                savedPayoutRequest.getUserId(),
                command.getAdminId());

        // Step 5: Map to response DTO
        return payoutRequestMapper.toResponse(savedPayoutRequest);
    }
}
