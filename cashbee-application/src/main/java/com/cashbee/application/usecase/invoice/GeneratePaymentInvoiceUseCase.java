package com.cashbee.application.usecase.invoice;

import com.cashbee.application.dto.invoice.GenerateInvoiceCommand;
import com.cashbee.application.dto.invoice.PaymentInvoiceResponse;
import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.domain.repository.PaymentInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Use case for generating payment invoices when batch transfers complete.
 *
 * Features:
 * - Generate unique invoice number
 * - Calculate platform breakdown
 * - Store invoice for user retrieval
 *
 * Invoice number format: INV-YYYYMMDD-XXXXX
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratePaymentInvoiceUseCase {

    private final PaymentInvoiceRepository invoiceRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQUENCE_COUNTER = new AtomicLong(0);

    /**
     * Generate a payment invoice for a completed batch transfer.
     *
     * @param command Invoice generation command
     * @return Generated invoice response
     */
    @Transactional
    public PaymentInvoiceResponse execute(GenerateInvoiceCommand command) {
        log.info("Generating payment invoice: userId={}, batchId={}, amount={}",
                command.getUserId(), command.getBatchId(), command.getAmount());

        // Generate unique invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Calculate platform breakdown
        BigDecimal shopeeAmount = BigDecimal.ZERO;
        BigDecimal lazadaAmount = BigDecimal.ZERO;
        BigDecimal tikiAmount = BigDecimal.ZERO;
        BigDecimal tiktokAmount = BigDecimal.ZERO;
        BigDecimal otherAmount = BigDecimal.ZERO;

        int shopeeOrders = 0;
        int lazadaOrders = 0;
        int tikiOrders = 0;
        int tiktokOrders = 0;
        int otherOrders = 0;

        if (command.getPlatformOrders() != null) {
            for (GenerateInvoiceCommand.PlatformOrderDetail detail : command.getPlatformOrders()) {
                String platform = detail.getPlatformCode().toLowerCase();
                switch (platform) {
                    case "shopee":
                        shopeeAmount = detail.getTotalAmount();
                        shopeeOrders = detail.getOrderCount();
                        break;
                    case "lazada":
                        lazadaAmount = detail.getTotalAmount();
                        lazadaOrders = detail.getOrderCount();
                        break;
                    case "tiki":
                        tikiAmount = detail.getTotalAmount();
                        tikiOrders = detail.getOrderCount();
                        break;
                    case "tiktok":
                        tiktokAmount = detail.getTotalAmount();
                        tiktokOrders = detail.getOrderCount();
                        break;
                    default:
                        otherAmount = otherAmount.add(detail.getTotalAmount());
                        otherOrders += detail.getOrderCount();
                        break;
                }
            }
        }

        // Create domain model
        PaymentInvoice invoice = PaymentInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .batchId(command.getBatchId())
                .batchItemId(command.getBatchItemId())
                .userId(command.getUserId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .accountNumber(command.getBankAccountNumber())
                .bankName(command.getBankName())
                .transferTime(command.getTransferTime())
                .transferStatus(command.getTransferStatus())
                .totalOrders(command.getTotalOrders())
                .shopeeAmount(shopeeAmount)
                .shopeeOrders(shopeeOrders)
                .lazadaAmount(lazadaAmount)
                .lazadaOrders(lazadaOrders)
                .tikiAmount(tikiAmount)
                .tikiOrders(tikiOrders)
                .tiktokAmount(tiktokAmount)
                .tiktokOrders(tiktokOrders)
                .otherAmount(otherAmount)
                .otherOrders(otherOrders)
                .emailSent(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Save invoice
        PaymentInvoice savedInvoice = invoiceRepository.save(invoice);

        log.info("Generated invoice: invoiceNumber={}, userId={}, amount={}",
                savedInvoice.getInvoiceNumber(), savedInvoice.getUserId(), savedInvoice.getAmount());

        return PaymentInvoiceResponse.fromDomain(savedInvoice);
    }

    /**
     * Generate unique invoice number.
     * Format: INV-YYYYMMDD-XXXXX
     *
     * Note: In production, this should use database sequence or distributed ID generator
     * for guaranteed uniqueness across multiple instances.
     */
    private String generateInvoiceNumber() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        long sequence = SEQUENCE_COUNTER.incrementAndGet();

        // Reset counter at midnight (simplified - in production use scheduled task)
        if (sequence > 99999) {
            SEQUENCE_COUNTER.set(1);
            sequence = 1;
        }

        return String.format("INV-%s-%05d", dateStr, sequence);
    }

    /**
     * Generate invoice number with database sequence (production-ready).
     * This method queries the database for the next sequence number.
     */
    public String generateInvoiceNumberFromDb() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String prefix = "INV-" + dateStr + "-";

        // Get the next sequence from existing invoices
        long nextSequence = invoiceRepository.countByInvoiceNumberStartingWith(prefix) + 1;

        return String.format("%s%05d", prefix, nextSequence);
    }
}
