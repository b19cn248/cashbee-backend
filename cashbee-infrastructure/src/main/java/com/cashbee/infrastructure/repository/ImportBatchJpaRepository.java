package com.cashbee.infrastructure.repository;

import com.cashbee.domain.enums.ImportStatus;
import com.cashbee.infrastructure.entity.ImportBatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for ImportBatchJpaEntity.
 *
 * @author CashBee Team
 */
@Repository
public interface ImportBatchJpaRepository extends JpaRepository<ImportBatchJpaEntity, Long> {

    List<ImportBatchJpaEntity> findByPlatformId(Long platformId);

    List<ImportBatchJpaEntity> findByStatus(ImportStatus status);

    List<ImportBatchJpaEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT b FROM ImportBatchJpaEntity b WHERE b.platformId = :platformId ORDER BY b.createdAt DESC LIMIT 1")
    Optional<ImportBatchJpaEntity> findLatestByPlatformId(@Param("platformId") Long platformId);

    List<ImportBatchJpaEntity> findByImportedBy(Long userId);

    long countByStatus(ImportStatus status);
}
