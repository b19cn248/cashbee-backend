package com.cashbee.domain.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Port (interface) for fetching product commission from external services.
 *
 * This is a domain service interface (port) that defines the contract
 * for getting product commission data. The actual implementation
 * (adapter) lives in the Infrastructure layer.
 *
 * Following Hexagonal Architecture:
 * - Domain defines the PORT (this interface)
 * - Infrastructure provides the ADAPTER (implementation)
 * - Application uses the PORT (depends on interface, not implementation)
 *
 * @author CashBee Team
 */
public interface ProductCommissionService {

    /**
     * Get commission information for a Shopee product.
     *
     * @param shopeeProductUrl Shopee product URL
     * @return Optional containing product commission info, empty if failed
     */
    Optional<ProductCommissionInfo> getProductCommission(String shopeeProductUrl);

    /**
     * DTO containing product commission information.
     * Defined as inner record to keep related types together.
     */
    record ProductCommissionInfo(
        String productName,
        String shopName,
        BigDecimal price,
        String imageUrl,
        String productLink,
        BigDecimal commission,      // Số tiền hoa hồng = (sellerRate + shopeeRate) * price
        BigDecimal commissionRate,  // Tỷ lệ hoa hồng (VD: 0.15 = 15%)
        Integer sales,              // Số lượt bán
        Boolean isCapped,
        BigDecimal maxCap
    ) {}
}
