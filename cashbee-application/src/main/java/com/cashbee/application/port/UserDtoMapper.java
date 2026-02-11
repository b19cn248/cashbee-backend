package com.cashbee.application.port;

import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.domain.model.User;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting between User domain model and application DTOs.
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
public interface UserDtoMapper {

    /**
     * Convert User domain model to UserResponse DTO.
     * Bank account fields (accountNumber, accountName, bankCode, bankName)
     * are ignored here and will be populated separately from UserBankAccount.
     *
     * @param user Domain model
     * @return Response DTO
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "userLevel", source = "userLevel", qualifiedByName = "userLevelToString")
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "accountName", ignore = true)
    @Mapping(target = "bankCode", ignore = true)
    @Mapping(target = "bankName", ignore = true)
    UserResponse toResponse(User user);

    /**
     * Convert list of User domain models to list of UserResponse DTOs.
     *
     * @param users List of domain models
     * @return List of response DTOs
     */
    List<UserResponse> toResponseList(List<User> users);

    /**
     * Convert UserStatus enum to String for DTO.
     *
     * @param status UserStatus enum
     * @return String representation
     */
    @Named("statusToString")
    default String statusToString(com.cashbee.domain.enums.UserStatus status) {
        return status != null ? status.name() : null;
    }

    /**
     * Convert UserLevel enum to String for DTO.
     *
     * @param userLevel UserLevel enum
     * @return String representation
     */
    @Named("userLevelToString")
    default String userLevelToString(com.cashbee.domain.enums.UserLevel userLevel) {
        return userLevel != null ? userLevel.name() : null;
    }
}
