package com.cashbee.infrastructure.repository;

import com.cashbee.domain.enums.ClickStatus;
import com.cashbee.infrastructure.entity.AffiliateClickJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for AffiliateClickJpaEntity.
 *
 * @author CashBee Team
 */
@Repository
public interface AffiliateClickJpaRepository extends JpaRepository<AffiliateClickJpaEntity, Long> {

    Optional<AffiliateClickJpaEntity> findByTrackingCode(String trackingCode);

    List<AffiliateClickJpaEntity> findByUserId(Long userId);

    List<AffiliateClickJpaEntity> findByUserIdAndPlatformId(Long userId, Long platformId);

    List<AffiliateClickJpaEntity> findByStatus(ClickStatus status);

    List<AffiliateClickJpaEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM AffiliateClickJpaEntity c WHERE c.createdAt < :cutoffDate AND c.orderMatched = false")
    List<AffiliateClickJpaEntity> findExpiredClicks(@Param("cutoffDate") LocalDateTime cutoffDate);

    long countByUserId(Long userId);

    long countByOrderMatchedTrue();

    @Query("SELECT (COUNT(c) * 1.0 / (SELECT COUNT(c2) FROM AffiliateClickJpaEntity c2 WHERE c2.status = 'CLICKED')) FROM AffiliateClickJpaEntity c WHERE c.orderMatched = true")
    Double calculateConversionRate();
}
