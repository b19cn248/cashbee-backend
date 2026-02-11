package com.cashbee.infrastructure.persistence.repository;

import com.cashbee.infrastructure.persistence.entity.PaymentInvoiceJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for PaymentInvoiceJpaEntity.
 *
 * @author CashBee Team
 */
@Repository
public interface PaymentInvoiceJpaRepository extends JpaRepository<PaymentInvoiceJpaEntity, Long> {

    /**
     * Find invoice by invoice number.
     */
    Optional<PaymentInvoiceJpaEntity> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find all invoices for a user with pagination.
     */
    Page<PaymentInvoiceJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find all invoices for a batch.
     */
    List<PaymentInvoiceJpaEntity> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    /**
     * Find all invoices for a batch item.
     */
    List<PaymentInvoiceJpaEntity> findByBatchItemId(Long batchItemId);

    /**
     * Count invoices created within a time range.
     * Used for generating sequence numbers.
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find all unsent invoices for email sending.
     */
    List<PaymentInvoiceJpaEntity> findByEmailSentFalseOrderByCreatedAtAsc();

    /**
     * Find unsent invoices for specific users.
     */
    @Query("SELECT p FROM PaymentInvoiceJpaEntity p WHERE p.emailSent = false AND p.userId IN :userIds ORDER BY p.createdAt ASC")
    List<PaymentInvoiceJpaEntity> findUnsentByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * Mark invoice as email sent.
     */
    @Modifying
    @Query("UPDATE PaymentInvoiceJpaEntity p SET p.emailSent = true, p.emailSentAt = :sentAt WHERE p.id = :id")
    void markEmailSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    /**
     * Mark invoice email as failed.
     */
    @Modifying
    @Query("UPDATE PaymentInvoiceJpaEntity p SET p.emailError = :error WHERE p.id = :id")
    void markEmailFailed(@Param("id") Long id, @Param("error") String error);

    /**
     * Check if invoice exists for a batch item.
     */
    boolean existsByBatchItemId(Long batchItemId);

    /**
     * Find latest invoice for a user.
     */
    Optional<PaymentInvoiceJpaEntity> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Count total invoices for a user.
     */
    long countByUserId(Long userId);

    /**
     * Find invoices by transfer status.
     */
    List<PaymentInvoiceJpaEntity> findByTransferStatus(String transferStatus);

    /**
     * Find invoices by user within date range.
     */
    Page<PaymentInvoiceJpaEntity> findByUserIdAndCreatedAtBetween(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    /**
     * Count invoices with invoice number starting with prefix.
     */
    long countByInvoiceNumberStartingWith(String prefix);
}
