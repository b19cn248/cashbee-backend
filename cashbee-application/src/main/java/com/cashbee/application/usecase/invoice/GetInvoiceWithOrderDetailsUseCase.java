package com.cashbee.application.usecase.invoice;

import com.cashbee.application.dto.invoice.InvoiceDetailResponse;
import com.cashbee.application.dto.invoice.InvoiceDetailResponse.*;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.PaymentInvoice;
import com.cashbee.domain.repository.CashbackRepository;
import com.cashbee.domain.repository.PaymentInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Use case for retrieving detailed invoice with order breakdown.
 *
 * This use case provides a "supermarket receipt" style view of an invoice,
 * showing every order and item that contributed to the total cashback.
 *
 * Data Flow:
 * 1. Load invoice by ID (validate ownership)
 * 2. Query cashbacks with order/item details using JOIN query
 * 3. Group results by order, then by item
 * 4. Calculate totals and build response
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetInvoiceWithOrderDetailsUseCase {

    private final PaymentInvoiceRepository invoiceRepository;
    private final CashbackRepository cashbackRepository;

    /**
     * Get detailed invoice with order breakdown.
     *
     * @param invoiceId Invoice ID
     * @param userId User ID (for ownership validation)
     * @return Detailed invoice response
     * @throws NotFoundException if invoice not found or does not belong to user
     */
    @Transactional(readOnly = true)
    public InvoiceDetailResponse execute(Long invoiceId, Long userId) {
        log.info("Getting invoice details: invoiceId={}, userId={}", invoiceId, userId);

        // 1. Load and validate invoice
        PaymentInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));

        // Validate ownership
        if (!invoice.getUserId().equals(userId)) {
            log.warn("Invoice {} does not belong to user {}", invoiceId, userId);
            throw new NotFoundException("Invoice not found: " + invoiceId);
        }

        // 2. Query cashbacks with order details
        List<Object[]> rawResults = cashbackRepository.findCashbacksWithOrderDetailsByBatchIdAndUserId(
                invoice.getBatchId(), userId);

        log.debug("Found {} cashback records for invoice {}", rawResults.size(), invoiceId);

        // 3. Group and transform results
        List<OrderDetail> orders = groupResultsByOrder(rawResults);

        // 4. Calculate summary
        InvoiceSummaryDetail summary = calculateSummary(orders, invoice);

        // 5. Build response
        return InvoiceDetailResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .transferTime(invoice.getTransferTime())
                .transferStatus(invoice.getTransferStatus())
                .createdAt(invoice.getCreatedAt())
                .summary(summary)
                .bankInfo(buildBankInfo(invoice))
                .orders(orders)
                .calculation(buildCalculationInfo())
                .build();
    }

    /**
     * Group raw query results by order code, then by item.
     */
    private List<OrderDetail> groupResultsByOrder(List<Object[]> rawResults) {
        // Use LinkedHashMap to preserve order (by order_time DESC)
        Map<String, OrderDetailBuilder> orderMap = new LinkedHashMap<>();

        for (Object[] row : rawResults) {
            // Extract fields from query result
            // Order: cashback_id, cashback_amount, cashback_commission, cashback_rate,
            //        order_internal_id, order_code, order_time, order_product_price, order_commission,
            //        item_id, item_name, shop_name, quantity, item_price, item_commission,
            //        category_lv1, img_url, platform_id, platform_name
            String orderCode = getString(row, 5);
            if (orderCode == null) {
                continue; // Skip invalid records
            }

            OrderDetailBuilder orderBuilder = orderMap.computeIfAbsent(orderCode, code -> {
                OrderDetailBuilder builder = new OrderDetailBuilder();
                builder.orderCode = code;
                builder.orderTime = getLocalDateTime(row, 6);
                builder.platformName = getString(row, 18);
                builder.items = new ArrayList<>();
                return builder;
            });

            // Add item details
            OrderItemDetail item = OrderItemDetail.builder()
                    .itemName(getString(row, 10))
                    .shopName(getString(row, 11))
                    .quantity(getInteger(row, 12))
                    .productPrice(getBigDecimal(row, 13))
                    .commission(getBigDecimal(row, 2))  // cashback_commission
                    .cashbackRate(getBigDecimal(row, 3))  // cashback_rate
                    .cashbackAmount(getBigDecimal(row, 1))  // cashback_amount
                    .imageUrl(getString(row, 16))
                    .category(getString(row, 15))
                    .build();

            // Handle null item name (for orders without item-level data)
            if (item.getItemName() == null || item.getItemName().isBlank()) {
                // Use order-level data as fallback
                item.setItemName("Sản phẩm từ đơn hàng " + orderCode);
                item.setProductPrice(getBigDecimal(row, 7)); // order_product_price
                item.setCommission(getBigDecimal(row, 8)); // order_commission
            }

            orderBuilder.items.add(item);
        }

        // Convert to list with calculated totals
        List<OrderDetail> orders = new ArrayList<>();
        for (OrderDetailBuilder builder : orderMap.values()) {
            OrderTotals totals = calculateOrderTotals(builder.items);
            orders.add(OrderDetail.builder()
                    .orderCode(builder.orderCode)
                    .orderTime(builder.orderTime)
                    .platformName(builder.platformName)
                    .items(builder.items)
                    .totals(totals)
                    .build());
        }

        return orders;
    }

    /**
     * Calculate totals for a single order.
     */
    private OrderTotals calculateOrderTotals(List<OrderItemDetail> items) {
        BigDecimal productAmount = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal cashbackAmount = BigDecimal.ZERO;

        for (OrderItemDetail item : items) {
            if (item.getProductPrice() != null) {
                productAmount = productAmount.add(item.getProductPrice());
            }
            if (item.getCommission() != null) {
                commission = commission.add(item.getCommission());
            }
            if (item.getCashbackAmount() != null) {
                cashbackAmount = cashbackAmount.add(item.getCashbackAmount());
            }
        }

        return OrderTotals.builder()
                .productAmount(productAmount)
                .commission(commission)
                .cashbackAmount(cashbackAmount)
                .build();
    }

    /**
     * Calculate invoice summary from orders.
     */
    private InvoiceSummaryDetail calculateSummary(List<OrderDetail> orders, PaymentInvoice invoice) {
        int totalOrders = orders.size();
        int totalItems = 0;
        BigDecimal totalProductAmount = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalCashback = BigDecimal.ZERO;

        for (OrderDetail order : orders) {
            totalItems += order.getItems().size();
            if (order.getTotals() != null) {
                if (order.getTotals().getProductAmount() != null) {
                    totalProductAmount = totalProductAmount.add(order.getTotals().getProductAmount());
                }
                if (order.getTotals().getCommission() != null) {
                    totalCommission = totalCommission.add(order.getTotals().getCommission());
                }
                if (order.getTotals().getCashbackAmount() != null) {
                    totalCashback = totalCashback.add(order.getTotals().getCashbackAmount());
                }
            }
        }

        // Calculate average cashback rate
        BigDecimal avgCashbackRate = BigDecimal.valueOf(80); // Default 80%
        if (totalCommission.compareTo(BigDecimal.ZERO) > 0 && totalCashback.compareTo(BigDecimal.ZERO) > 0) {
            avgCashbackRate = totalCashback
                    .divide(totalCommission, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal platformFeeRate = BigDecimal.valueOf(100).subtract(avgCashbackRate);

        return InvoiceSummaryDetail.builder()
                .totalOrders(totalOrders)
                .totalItems(totalItems)
                .totalProductAmount(totalProductAmount)
                .totalCommission(totalCommission)
                .cashbackRate(avgCashbackRate)
                .platformFeeRate(platformFeeRate)
                .totalCashback(totalCashback)
                .currency(invoice.getCurrency() != null ? invoice.getCurrency() : "VND")
                .build();
    }

    /**
     * Build bank info from invoice.
     */
    private BankInfo buildBankInfo(PaymentInvoice invoice) {
        return BankInfo.builder()
                .bankName(invoice.getBankName())
                .bankFullName(invoice.getBankFullName())
                .accountNumber(invoice.getMaskedAccountNumber()) // Show masked for security
                .accountName(invoice.getAccountName())
                .build();
    }

    /**
     * Build calculation explanation.
     */
    private CalculationInfo buildCalculationInfo() {
        return CalculationInfo.builder()
                .description("Tiền hoàn được tính dựa trên hoa hồng từ các nền tảng thương mại điện tử")
                .formula("Tiền hoàn = Hoa hồng × Tỷ lệ hoàn (thường là 80%)")
                .note("Tỷ lệ hoàn có thể khác nhau tùy theo sản phẩm và chương trình khuyến mãi")
                .build();
    }

    // ========== Helper methods for safe type conversion ==========

    private String getString(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return null;
        }
        return row[index].toString();
    }

    private Integer getInteger(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return null;
        }
        Object val = row[index];
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return null;
        }
        Object val = row[index];
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTime(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return null;
        }
        Object val = row[index];
        if (val instanceof LocalDateTime) {
            return (LocalDateTime) val;
        }
        if (val instanceof Timestamp) {
            return ((Timestamp) val).toLocalDateTime();
        }
        return null;
    }

    /**
     * Helper class for building OrderDetail.
     */
    private static class OrderDetailBuilder {
        String orderCode;
        LocalDateTime orderTime;
        String platformName;
        List<OrderItemDetail> items;
    }
}
