package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.OtpPurpose;
import com.cashbee.domain.model.OtpVerification;
import com.cashbee.domain.repository.OtpVerificationRepository;
import com.cashbee.infrastructure.persistence.entity.OtpVerificationEntity.OtpPurposeEntity;
import com.cashbee.infrastructure.persistence.mapper.OtpVerificationMapper;
import com.cashbee.infrastructure.persistence.repository.OtpVerificationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of OtpVerificationRepository using Spring Data JPA.
 * Follows the Adapter pattern in Hexagonal Architecture.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpVerificationRepositoryAdapter implements OtpVerificationRepository {

    private final OtpVerificationJpaRepository jpaRepository;
    private final OtpVerificationMapper mapper;

    @Override
    public OtpVerification save(OtpVerification otpVerification) {
        log.debug("Saving OTP verification for email: {}, purpose: {}",
            otpVerification.getEmail(), otpVerification.getPurpose());

        var entity = mapper.toEntity(otpVerification);
        var savedEntity = jpaRepository.save(entity);
        var savedDomain = mapper.toDomain(savedEntity);

        log.debug("OTP verification saved with ID: {}", savedDomain.getId());
        return savedDomain;
    }

    @Override
    public Optional<OtpVerification> findById(Long id) {
        log.debug("Finding OTP verification by ID: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<OtpVerification> findLatestByEmailAndPurpose(String email, OtpPurpose purpose) {
        log.debug("Finding latest OTP verification for email: {}, purpose: {}", email, purpose);

        var purposeEntity = mapper.mapPurpose(purpose);
        return jpaRepository.findLatestByEmailAndPurpose(email, purposeEntity)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<OtpVerification> findByEmailAndPurposeAndOtpCode(String email, OtpPurpose purpose, String otpCode) {
        log.debug("Finding OTP verification for email: {}, purpose: {}, code: ***", email, purpose);

        var purposeEntity = mapper.mapPurpose(purpose);
        return jpaRepository.findByEmailAndPurposeAndOtpCode(email, purposeEntity, otpCode)
            .map(mapper::toDomain);
    }

    @Override
    public List<OtpVerification> findExpiredAndUnverified(LocalDateTime expiredBefore) {
        log.debug("Finding expired and unverified OTPs before: {}", expiredBefore);

        return jpaRepository.findExpiredAndUnverified(expiredBefore).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<OtpVerification> findByKeycloakId(String keycloakId) {
        log.debug("Finding OTP verification by Keycloak ID: {}", keycloakId);

        return jpaRepository.findByKeycloakId(keycloakId)
            .map(mapper::toDomain);
    }

    @Override
    public long countByEmailAndPurposeSince(String email, OtpPurpose purpose, LocalDateTime since) {
        log.debug("Counting OTPs for email: {}, purpose: {}, since: {}", email, purpose, since);

        var purposeEntity = mapper.mapPurpose(purpose);
        long count = jpaRepository.countByEmailAndPurposeSince(email, purposeEntity, since);

        log.debug("Found {} OTP(s)", count);
        return count;
    }

    @Override
    public void delete(OtpVerification otpVerification) {
        log.debug("Deleting OTP verification with ID: {}", otpVerification.getId());

        var entity = mapper.toEntity(otpVerification);
        jpaRepository.delete(entity);

        log.debug("OTP verification deleted");
    }

    @Override
    public void deleteAll(List<OtpVerification> otpVerifications) {
        log.debug("Deleting {} OTP verification(s)", otpVerifications.size());

        var entities = otpVerifications.stream()
            .map(mapper::toEntity)
            .collect(Collectors.toList());
        jpaRepository.deleteAll(entities);

        log.debug("OTP verifications deleted");
    }

    @Override
    public boolean existsByEmailAndPurpose(String email, OtpPurpose purpose) {
        log.debug("Checking if OTP exists for email: {}, purpose: {}", email, purpose);

        var purposeEntity = mapper.mapPurpose(purpose);
        return jpaRepository.existsByEmailAndPurpose(email, purposeEntity);
    }
}
