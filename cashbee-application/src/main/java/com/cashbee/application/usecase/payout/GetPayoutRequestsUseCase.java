package com.cashbee.application.usecase.payout;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.payout.GetPayoutRequestsQuery;
import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.application.port.PayoutRequestMapper;
import com.cashbee.domain.model.PayoutRequest;
import com.cashbee.domain.repository.PayoutRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for getting payout requests with pagination and filtering.
 *
 * Business Scenarios:
 * 1. User views their own payout requests (filtered by userId)
 * 2. Admin views all payout requests (userId = null)
 * 3. Filter by status (REQUESTED, PROCESSING, PAID, REJECTED, CANCELLED)
 * 4. Combine userId + status for specific filtering
 *
 * Flow:
 * 1. Validate query
 * 2. Determine filtering strategy based on userId and status
 * 3. Fetch payout requests from repository with pagination
 * 4. Fetch total count for pagination metadata
 * 5. Map domain models to DTOs
 * 6. Build and return PageResponse
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetPayoutRequestsUseCase {

    private final PayoutRequestRepository payoutRequestRepository;
    private final PayoutRequestMapper payoutRequestMapper;

    /**
     * Execute get payout requests operation.
     *
     * @param query Query with optional filters and pagination
     * @return Paginated payout request responses
     * @throws NullPointerException if query is null
     */
    @Transactional(readOnly = true)
    public PageResponse<PayoutRequestResponse> execute(GetPayoutRequestsQuery query) {
        log.info("Getting payout requests: userId={}, status={}, page={}, size={}",
                query.getUserId(), query.getStatus(), query.getPage(), query.getSize());

        // Step 1: Validate query (null check)
        if (query == null) {
            throw new NullPointerException("GetPayoutRequestsQuery cannot be null");
        }

        // Step 2 & 3: Fetch payout requests and count based on filters
        List<PayoutRequest> payoutRequests;
        long totalElements;

        if (query.getUserId() != null && query.getStatus() != null) {
            // Both userId and status provided
            log.debug("Fetching payout requests by userId={} and status={}", query.getUserId(), query.getStatus());
            payoutRequests = payoutRequestRepository.findByUserIdAndStatus(
                    query.getUserId(),
                    query.getStatus(),
                    query.getPage(),
                    query.getSize()
            );
            totalElements = payoutRequestRepository.countByUserIdAndStatus(query.getUserId(), query.getStatus());
        } else if (query.getUserId() != null) {
            // Only userId provided
            log.debug("Fetching payout requests by userId={}", query.getUserId());
            payoutRequests = payoutRequestRepository.findByUserId(
                    query.getUserId(),
                    query.getPage(),
                    query.getSize()
            );
            totalElements = payoutRequestRepository.countByUserId(query.getUserId());
        } else if (query.getStatus() != null) {
            // Only status provided
            log.debug("Fetching payout requests by status={}", query.getStatus());
            payoutRequests = payoutRequestRepository.findByStatus(
                    query.getStatus(),
                    query.getPage(),
                    query.getSize()
            );
            totalElements = payoutRequestRepository.countByStatus(query.getStatus());
        } else {
            // No filters - get all (admin view)
            log.debug("Fetching all payout requests");
            payoutRequests = payoutRequestRepository.findAll(query.getPage(), query.getSize());
            totalElements = payoutRequestRepository.countAll();
        }

        // Step 4: Map to response DTOs
        List<PayoutRequestResponse> responses = payoutRequests.stream()
                .map(payoutRequestMapper::toResponse)
                .toList();

        // Step 5: Build PageResponse
        PageResponse<PayoutRequestResponse> pageResponse = PageResponse.of(
                responses,
                query.getPage(),
                query.getSize(),
                totalElements
        );

        log.info("Found {} payout requests (page {}/{})",
                totalElements,
                pageResponse.getPage() + 1,
                pageResponse.getTotalPages());

        // Step 6: Return paginated results
        return pageResponse;
    }
}
