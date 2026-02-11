package com.cashbee.infrastructure.persistence.adapter;

import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.domain.repository.PaymentInvoiceRepository;
import com.cashbee.infrastructure.persistence.entity.PaymentInvoiceJpaEntity;
import com.cashbee.infrastructure.persistence.mapper.PaymentInvoiceMapper;
import com.cashbee.infrastructure.persistence.repository.PaymentInvoiceJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation of PaymentInvoiceRepository.
 *
 * This adapter bridges the domain layer with the infrastructure layer,
 * converting between domain models and JPA entities.
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentInvoiceRepositoryAdapter implements PaymentInvoiceRepository {

    private final PaymentInvoiceJpaRepository jpaRepository;

    @Override
    public PaymentInvoice save(PaymentInvoice invoice) {
        log.debug("Saving payment invoice: {}", invoice.getInvoiceNumber());
        PaymentInvoiceJpaEntity entity = PaymentInvoiceMapper.toEntity(invoice);
        PaymentInvoiceJpaEntity savedEntity = jpaRepository.save(entity);
        return PaymentInvoiceMapper.toDomain(savedEntity);
    }

    @Override
    public List<PaymentInvoice> saveAll(List<PaymentInvoice> invoices) {
        log.debug("Saving {} payment invoices", invoices.size());
        List<PaymentInvoiceJpaEntity> entities = invoices.stream()
                .map(PaymentInvoiceMapper::toEntity)
                .collect(Collectors.toList());
        List<PaymentInvoiceJpaEntity> savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(PaymentInvoiceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PaymentInvoice> findById(Long id) {
        return jpaRepository.findById(id)
                .map(PaymentInvoiceMapper::toDomain);
    }

    @Override
    public Optional<PaymentInvoice> findByInvoiceNumber(String invoiceNumber) {
        return jpaRepository.findByInvoiceNumber(invoiceNumber)
                .map(PaymentInvoiceMapper::toDomain);
    }

    @Override
    public Page<PaymentInvoice> findByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PaymentInvoiceMapper::toDomain);
    }

    @Override
    public List<PaymentInvoice> findByBatchId(Long batchId) {
        return jpaRepository.findByBatchIdOrderByCreatedAtDesc(batchId).stream()
                .map(PaymentInvoiceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentInvoice> findByBatchItemId(Long batchItemId) {
        return jpaRepository.findByBatchItemId(batchItemId).stream()
                .map(PaymentInvoiceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return jpaRepository.countByCreatedAtBetween(startTime, endTime);
    }

    @Override
    public List<PaymentInvoice> findUnsentInvoices() {
        return jpaRepository.findByEmailSentFalseOrderByCreatedAtAsc().stream()
                .map(PaymentInvoiceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentInvoice> findUnsentInvoicesForUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findUnsentByUserIds(userIds).stream()
                .map(PaymentInvoiceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void markEmailSent(Long invoiceId, LocalDateTime sentAt) {
        log.debug("Marking invoice {} as email sent at {}", invoiceId, sentAt);
        jpaRepository.markEmailSent(invoiceId, sentAt);
    }

    @Override
    public void markEmailFailed(Long invoiceId, String error) {
        log.warn("Marking invoice {} email as failed: {}", invoiceId, error);
        jpaRepository.markEmailFailed(invoiceId, error);
    }

    @Override
    public boolean existsByBatchItemId(Long batchItemId) {
        return jpaRepository.existsByBatchItemId(batchItemId);
    }

    @Override
    public Optional<PaymentInvoice> findLatestByUserId(Long userId) {
        return jpaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(PaymentInvoiceMapper::toDomain);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public Page<PaymentInvoice> findByUserIdAndCreatedAtBetween(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        return jpaRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate, pageable)
                .map(PaymentInvoiceMapper::toDomain);
    }

    @Override
    public long countByInvoiceNumberStartingWith(String prefix) {
        return jpaRepository.countByInvoiceNumberStartingWith(prefix);
    }
}
