package com.cashbee.application.port;

import com.cashbee.application.dto.wallet.WalletResponse;
import com.cashbee.domain.model.UserWallet;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between UserWallet domain model and application DTOs.
 *
 * This is used in the application layer to convert domain models to DTOs
 * that are exposed to the presentation layer.
 *
 * @author CashBee Team
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface WalletMapper {

    /**
     * Convert UserWallet domain model to WalletResponse DTO.
     *
     * @param wallet Domain model
     * @return Response DTO
     */
    WalletResponse toResponse(UserWallet wallet);

    /**
     * Convert list of UserWallet domain models to list of WalletResponse DTOs.
     *
     * @param wallets List of domain models
     * @return List of response DTOs
     */
    List<WalletResponse> toResponseList(List<UserWallet> wallets);
}
