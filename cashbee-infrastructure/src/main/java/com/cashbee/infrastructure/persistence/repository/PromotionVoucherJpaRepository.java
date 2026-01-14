package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import com.cashbee.infrastructure.persistence.entity.PromotionVoucherJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for PromotionVoucher.
 *
 * @author CashBee Team
 */
@Repository
public interface PromotionVoucherJpaRepository extends JpaRepository<PromotionVoucherJpaEntity, Long> {

    /**
     * Find voucher by voucher code.
     *
     * @param code Voucher code
     * @return Optional containing voucher entity
     */
    Optional<PromotionVoucherJpaEntity> findByCode(String code);

    /**
     * Check if voucher exists by code.
     *
     * @param code Voucher code
     * @return true if voucher with code exists
     */
    boolean existsByCode(String code);

    /**
     * Find vouchers by status with pagination.
     *
     * @param status Voucher status
     * @param pageable Pagination info
     * @return Page of vouchers
     */
    Page<PromotionVoucherJpaEntity> findByStatus(VoucherStatus status, Pageable pageable);

    /**
     * Find vouchers by status and category with pagination.
     *
     * @param status Voucher status
     * @param category Voucher category
     * @param pageable Pagination info
     * @return Page of vouchers
     */
    Page<PromotionVoucherJpaEntity> findByStatusAndCategory(
        VoucherStatus status,
        VoucherCategory category,
        Pageable pageable
    );

    /**
     * Find vouchers by status and platform with pagination.
     *
     * @param status Voucher status
     * @param platform Platform name
     * @param pageable Pagination info
     * @return Page of vouchers
     */
    Page<PromotionVoucherJpaEntity> findByStatusAndPlatform(
        VoucherStatus status,
        String platform,
        Pageable pageable
    );

    /**
     * Find vouchers by status, category and platform with pagination.
     *
     * @param status Voucher status
     * @param category Voucher category
     * @param platform Platform name
     * @param pageable Pagination info
     * @return Page of vouchers
     */
    Page<PromotionVoucherJpaEntity> findByStatusAndCategoryAndPlatform(
        VoucherStatus status,
        VoucherCategory category,
        String platform,
        Pageable pageable
    );

    /**
     * Count vouchers by status.
     *
     * @param status Voucher status
     * @return Number of vouchers
     */
    long countByStatus(VoucherStatus status);

    /**
     * Increment view count for voucher.
     *
     * @param id Voucher ID
     * @return Number of rows updated
     */
    @Modifying
    @Query("UPDATE PromotionVoucherJpaEntity v SET v.viewCount = v.viewCount + 1, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id = :id")
    int incrementViewCount(@Param("id") Long id);

    /**
     * Increment click count for voucher.
     *
     * @param id Voucher ID
     * @return Number of rows updated
     */
    @Modifying
    @Query("UPDATE PromotionVoucherJpaEntity v SET v.clickCount = v.clickCount + 1, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id = :id")
    int incrementClickCount(@Param("id") Long id);
}
