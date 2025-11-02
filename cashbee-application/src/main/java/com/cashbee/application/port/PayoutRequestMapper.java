package com.cashbee.application.port;

import com.cashbee.application.dto.payout.PayoutRequestResponse;
import com.cashbee.domain.model.PayoutRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for PayoutRequest domain model ↔ DTOs.
 *
 * Converts between:
 * - Domain model (PayoutRequest) - pure business logic
 * - Response DTO (PayoutRequestResponse) - API response
 *
 * @author CashBee Team
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PayoutRequestMapper {

    /**
     * Convert domain model to response DTO.
     *
     * @param payoutRequest Domain model
     * @return Response DTO
     */
    PayoutRequestResponse toResponse(PayoutRequest payoutRequest);

    /**
     * Convert list of domain models to list of response DTOs.
     *
     * @param payoutRequests List of domain models
     * @return List of response DTOs
     */
    List<PayoutRequestResponse> toResponseList(List<PayoutRequest> payoutRequests);
}
