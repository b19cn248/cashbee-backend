package com.cashbee.infrastructure.persistence.mapper;

import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.infrastructure.persistence.entity.PaymentInvoiceJpaEntity;

/**
 * Mapper between PaymentInvoice domain model and PaymentInvoiceJpaEntity.
 *
 * This mapper converts between domain objects (business logic)
 * and JPA entities (database persistence).
 *
 * @author CashBee Team
 */
public class PaymentInvoiceMapper {

    /**
     * Convert JPA entity to domain model.
     *
     * @param entity JPA entity
     * @return Domain model
     */
    public static PaymentInvoice toDomain(PaymentInvoiceJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentInvoice.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .userId(entity.getUserId())
                .batchId(entity.getBatchId())
                .batchItemId(entity.getBatchItemId())
                .transactionId(entity.getTransactionId())
                // Payment details
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .transferStatus(entity.getTransferStatus())
                .transferTime(entity.getTransferTime())
                // User info snapshot
                .userFullName(entity.getUserFullName())
                .userEmail(entity.getUserEmail())
                .userPhone(entity.getUserPhone())
                // Bank info snapshot
                .bankName(entity.getBankName())
                .bankFullName(entity.getBankFullName())
                .accountNumber(entity.getAccountNumber())
                .accountName(entity.getAccountName())
                // Cashback summary
                .totalOrders(entity.getTotalOrders())
                .totalCashbackAmount(entity.getTotalCashbackAmount())
                .shopeeOrders(entity.getShopeeOrders())
                .shopeeAmount(entity.getShopeeAmount())
                .lazadaOrders(entity.getLazadaOrders())
                .lazadaAmount(entity.getLazadaAmount())
                .tikiOrders(entity.getTikiOrders())
                .tikiAmount(entity.getTikiAmount())
                .tiktokOrders(entity.getTiktokOrders())
                .tiktokAmount(entity.getTiktokAmount())
                .otherOrders(entity.getOtherOrders())
                .otherAmount(entity.getOtherAmount())
                // Additional info
                .description(entity.getDescription())
                .remark(entity.getRemark())
                // Email tracking
                .emailSent(entity.getEmailSent())
                .emailSentAt(entity.getEmailSentAt())
                .emailError(entity.getEmailError())
                // Timestamps
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert domain model to JPA entity.
     *
     * @param domain Domain model
     * @return JPA entity
     */
    public static PaymentInvoiceJpaEntity toEntity(PaymentInvoice domain) {
        if (domain == null) {
            return null;
        }

        return PaymentInvoiceJpaEntity.builder()
                .id(domain.getId())
                .invoiceNumber(domain.getInvoiceNumber())
                .userId(domain.getUserId())
                .batchId(domain.getBatchId())
                .batchItemId(domain.getBatchItemId())
                .transactionId(domain.getTransactionId())
                // Payment details
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .transferStatus(domain.getTransferStatus())
                .transferTime(domain.getTransferTime())
                // User info snapshot
                .userFullName(domain.getUserFullName())
                .userEmail(domain.getUserEmail())
                .userPhone(domain.getUserPhone())
                // Bank info snapshot
                .bankName(domain.getBankName())
                .bankFullName(domain.getBankFullName())
                .accountNumber(domain.getAccountNumber())
                .accountName(domain.getAccountName())
                // Cashback summary
                .totalOrders(domain.getTotalOrders())
                .totalCashbackAmount(domain.getTotalCashbackAmount())
                .shopeeOrders(domain.getShopeeOrders())
                .shopeeAmount(domain.getShopeeAmount())
                .lazadaOrders(domain.getLazadaOrders())
                .lazadaAmount(domain.getLazadaAmount())
                .tikiOrders(domain.getTikiOrders())
                .tikiAmount(domain.getTikiAmount())
                .tiktokOrders(domain.getTiktokOrders())
                .tiktokAmount(domain.getTiktokAmount())
                .otherOrders(domain.getOtherOrders())
                .otherAmount(domain.getOtherAmount())
                // Additional info
                .description(domain.getDescription())
                .remark(domain.getRemark())
                // Email tracking
                .emailSent(domain.getEmailSent())
                .emailSentAt(domain.getEmailSentAt())
                .emailError(domain.getEmailError())
                // Timestamps
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    // Private constructor to prevent instantiation
    private PaymentInvoiceMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
}
