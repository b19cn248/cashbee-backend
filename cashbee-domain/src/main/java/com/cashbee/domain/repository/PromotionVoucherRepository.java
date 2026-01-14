package com.cashbee.domain.repository;

import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import com.cashbee.domain.model.PromotionVoucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PromotionVoucher entity.
 * Defines operations for voucher data access.
 *
 * This is a port (Hexagonal Architecture) that will be implemented
 * by the infrastructure layer (adapter).
 *
 * @author CashBee Team
 */
public interface PromotionVoucherRepository {

    /**
     * Find voucher by ID.
     *
     * @param id Voucher ID
     * @return Optional containing PromotionVoucher if found
     */
    Optional<PromotionVoucher> findById(Long id);

    /**
     * Find voucher by voucher code.
     *
     * @param code Voucher code (e.g., "AFFZOSI0")
     * @return Optional containing PromotionVoucher if found
     */
    Optional<PromotionVoucher> findByCode(String code);

    /**
     * Find all vouchers.
     *
     * @return List of all vouchers
     */
    List<PromotionVoucher> findAll();

    /**
     * Find vouchers with pagination.
     *
     * @param pageable Pagination info
     * @return Page of vouchers
     */
    Page<PromotionVoucher> findAll(Pageable pageable);

    /**
     * Find vouchers by status with pagination.
     *
     * @param status Voucher status
     * @param pageable Pagination info
     * @return Page of vouchers with given status
     */
    Page<PromotionVoucher> findByStatus(VoucherStatus status, Pageable pageable);

    /**
     * Find vouchers by status and category with pagination.
     *
     * @param status Voucher status
     * @param category Voucher category
     * @param pageable Pagination info
     * @return Page of matching vouchers
     */
    Page<PromotionVoucher> findByStatusAndCategory(VoucherStatus status, VoucherCategory category, Pageable pageable);

    /**
     * Find vouchers by status and platform with pagination.
     *
     * @param status Voucher status
     * @param platform Platform (e.g., "SHOPEE")
     * @param pageable Pagination info
     * @return Page of matching vouchers
     */
    Page<PromotionVoucher> findByStatusAndPlatform(VoucherStatus status, String platform, Pageable pageable);

    /**
     * Find vouchers by status, category and platform with pagination.
     *
     * @param status Voucher status
     * @param category Voucher category
     * @param platform Platform
     * @param pageable Pagination info
     * @return Page of matching vouchers
     */
    Page<PromotionVoucher> findByStatusAndCategoryAndPlatform(
        VoucherStatus status,
        VoucherCategory category,
        String platform,
        Pageable pageable
    );

    /**
     * Find active vouchers with pagination.
     *
     * @param pageable Pagination info
     * @return Page of active vouchers
     */
    Page<PromotionVoucher> findByStatusActive(Pageable pageable);

    /**
     * Save voucher (create or update).
     *
     * @param voucher Voucher to save
     * @return Saved voucher with generated ID
     */
    PromotionVoucher save(PromotionVoucher voucher);

    /**
     * Save multiple vouchers (batch save).
     *
     * @param vouchers List of vouchers to save
     * @return List of saved vouchers
     */
    List<PromotionVoucher> saveAll(List<PromotionVoucher> vouchers);

    /**
     * Delete voucher by ID.
     *
     * @param id Voucher ID
     */
    void deleteById(Long id);

    /**
     * Check if voucher exists by ID.
     *
     * @param id Voucher ID
     * @return true if voucher with ID exists
     */
    boolean existsById(Long id);

    /**
     * Check if voucher exists by code.
     *
     * @param code Voucher code
     * @return true if voucher with code exists
     */
    boolean existsByCode(String code);

    /**
     * Count vouchers by status.
     *
     * @param status Voucher status
     * @return Number of vouchers with given status
     */
    long countByStatus(VoucherStatus status);

    /**
     * Count all vouchers.
     *
     * @return Total number of vouchers
     */
    long count();

    /**
     * Increment view count for voucher.
     *
     * @param id Voucher ID
     * @return Updated view count
     */
    int incrementViewCount(Long id);

    /**
     * Increment click count for voucher.
     *
     * @param id Voucher ID
     * @return Updated click count
     */
    int incrementClickCount(Long id);
}
