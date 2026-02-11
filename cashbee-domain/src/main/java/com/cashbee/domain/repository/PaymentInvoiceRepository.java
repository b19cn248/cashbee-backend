package com.cashbee.domain.repository;

import com.cashbee.domain.model.PaymentInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PaymentInvoice domain model.
 *
 * This is a domain-level interface (port in hexagonal architecture).
 * Implementation will be in infrastructure layer.
 *
 * @author CashBee Team
 */
public interface PaymentInvoiceRepository {

    /**
     * Save payment invoice.
     *
     * @param invoice Invoice to save
     * @return Saved invoice
     */
    PaymentInvoice save(PaymentInvoice invoice);

    /**
     * Save multiple invoices (batch insert).
     *
     * @param invoices List of invoices to save
     * @return List of saved invoices
     */
    List<PaymentInvoice> saveAll(List<PaymentInvoice> invoices);

    /**
     * Find invoice by ID.
     *
     * @param id Invoice ID
     * @return Invoice if found
     */
    Optional<PaymentInvoice> findById(Long id);

    /**
     * Find invoice by invoice number.
     *
     * @param invoiceNumber Invoice number (e.g., INV-20260104-00001)
     * @return Invoice if found
     */
    Optional<PaymentInvoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find all invoices for a user (paginated).
     * Ordered by created_at DESC (newest first).
     *
     * @param userId User ID
     * @param pageable Pagination info
     * @return Page of invoices
     */
    Page<PaymentInvoice> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all invoices for a batch.
     *
     * @param batchId Batch ID
     * @return List of invoices
     */
    List<PaymentInvoice> findByBatchId(Long batchId);

    /**
     * Count invoices created between two timestamps.
     * Used to generate invoice number (INV-YYYYMMDD-XXXXX).
     *
     * @param startTime Start time (inclusive)
     * @param endTime End time (exclusive)
     * @return Count of invoices
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find all invoices for a batch item.
     *
     * @param batchItemId Batch item ID
     * @return List of invoices
     */
    List<PaymentInvoice> findByBatchItemId(Long batchItemId);

    /**
     * Find invoices where email has not been sent yet.
     * Used for retry/batch email sending.
     *
     * @return List of invoices pending email
     */
    List<PaymentInvoice> findUnsentInvoices();

    /**
     * Find unsent invoices for specific users.
     *
     * @param userIds List of user IDs
     * @return List of unsent invoices
     */
    List<PaymentInvoice> findUnsentInvoicesForUsers(List<Long> userIds);

    /**
     * Find invoices by user within date range.
     *
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination info
     * @return Page of invoices
     */
    Page<PaymentInvoice> findByUserIdAndCreatedAtBetween(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    /**
     * Check if invoice exists for a batch item.
     * Used to prevent duplicate invoice creation.
     *
     * @param batchItemId Batch item ID
     * @return true if exists
     */
    boolean existsByBatchItemId(Long batchItemId);

    /**
     * Mark invoice as email sent.
     *
     * @param invoiceId Invoice ID
     * @param sentAt Timestamp when email was sent
     */
    void markEmailSent(Long invoiceId, LocalDateTime sentAt);

    /**
     * Mark invoice email as failed.
     *
     * @param invoiceId Invoice ID
     * @param error Error message
     */
    void markEmailFailed(Long invoiceId, String error);

    /**
     * Find latest invoice for a user.
     *
     * @param userId User ID
     * @return Latest invoice if found
     */
    Optional<PaymentInvoice> findLatestByUserId(Long userId);

    /**
     * Count total invoices for a user.
     *
     * @param userId User ID
     * @return Count of invoices
     */
    long countByUserId(Long userId);

    /**
     * Count invoices with invoice number starting with prefix.
     * Used for generating unique invoice numbers.
     *
     * @param prefix Invoice number prefix (e.g., "INV-20260104-")
     * @return Count of invoices
     */
    long countByInvoiceNumberStartingWith(String prefix);

    /**
     * Get next sequence number for invoice generation.
     * Returns count + 1 for the current day.
     *
     * @return Next sequence number
     */
    default long getNextSequenceForToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return countByCreatedAtBetween(startOfDay, endOfDay) + 1;
    }
}
