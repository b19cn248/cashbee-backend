package com.cashbee.application.port;

import com.cashbee.application.dto.transaction.TransactionResponse;
import com.cashbee.domain.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Transaction domain model ↔ DTOs.
 *
 * Converts between:
 * - Domain model (Transaction) - pure business logic
 * - Response DTO (TransactionResponse) - API response
 *
 * @author CashBee Team
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TransactionMapper {

    /**
     * Convert domain model to response DTO.
     *
     * @param transaction Domain model
     * @return Response DTO
     */
    TransactionResponse toResponse(Transaction transaction);

    /**
     * Convert list of domain models to list of response DTOs.
     *
     * @param transactions List of domain models
     * @return List of response DTOs
     */
    List<TransactionResponse> toResponseList(List<Transaction> transactions);
}
