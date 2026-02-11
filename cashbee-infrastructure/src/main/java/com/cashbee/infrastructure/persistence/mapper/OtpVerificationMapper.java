package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.infrastructure.persistence.entity.OtpVerificationEntity;
import com.cashbee.infrastructure.persistence.entity.OtpVerificationEntity.OtpPurposeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for converting between OtpVerification domain model and OtpVerificationEntity.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OtpVerificationMapper {

    /**
     * Convert domain model to JPA entity
     */
    @Mapping(target = "purpose", source = "purpose")
    OtpVerificationEntity toEntity(OtpVerification otpVerification);

    /**
     * Convert JPA entity to domain model
     */
    @Mapping(target = "purpose", source = "purpose")
    OtpVerification toDomain(OtpVerificationEntity entity);

    /**
     * Map domain OtpPurpose to entity OtpPurposeEntity
     */
    default OtpPurposeEntity mapPurpose(OtpPurpose purpose) {
        if (purpose == null) {
            return null;
        }
        return switch (purpose) {
            case REGISTRATION -> OtpPurposeEntity.REGISTRATION;
            case PASSWORD_RESET -> OtpPurposeEntity.PASSWORD_RESET;
            case EMAIL_CHANGE -> OtpPurposeEntity.EMAIL_CHANGE;
            case TWO_FACTOR_AUTH -> OtpPurposeEntity.TWO_FACTOR_AUTH;
        };
    }

    /**
     * Map entity OtpPurposeEntity to domain OtpPurpose
     */
    default OtpPurpose mapPurpose(OtpPurposeEntity purposeEntity) {
        if (purposeEntity == null) {
            return null;
        }
        return switch (purposeEntity) {
            case REGISTRATION -> OtpPurpose.REGISTRATION;
            case PASSWORD_RESET -> OtpPurpose.PASSWORD_RESET;
            case EMAIL_CHANGE -> OtpPurpose.EMAIL_CHANGE;
            case TWO_FACTOR_AUTH -> OtpPurpose.TWO_FACTOR_AUTH;
        };
    }
}
