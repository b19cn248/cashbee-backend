package com.cashbee.infrastructure.external.tui3gang;

import com.cashbee.domain.service.ProductCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adapter that implements ProductCommissionService using Tui3Gang API.
 *
 * This is the ADAPTER in Hexagonal Architecture:
 * - Implements the PORT (ProductCommissionService interface from Domain)
 * - Uses Tui3GangService to call external API
 * - Converts external DTOs to domain DTOs
 *
 * Commission calculation:
 * - commissionRate = sellerCommissionRate + shopeeCommissionRate
 * - commission = commissionRate * price
 *
 * Note: No longer {@code @Primary}. Default commission source is
 * ShoppingTietKiemProductCommissionAdapter (Tui3Gang API is unavailable).
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Tui3GangProductCommissionAdapter implements ProductCommissionService {

    private final Tui3GangService tui3GangService;

    @Override
    public Optional<ProductCommissionInfo> getProductCommission(String shopeeProductUrl) {
        log.info("Tui3GangAdapter: Getting commission for URL: {}", shopeeProductUrl);

        // Call Tui3Gang API
        Optional<Tui3GangProductInfo> productInfoOpt = tui3GangService.getProductInfo(shopeeProductUrl);

        if (productInfoOpt.isEmpty()) {
            log.warn("Tui3GangAdapter: No product data returned for URL: {}", shopeeProductUrl);
            return Optional.empty();
        }

        // Convert to domain DTO
        Tui3GangProductInfo info = productInfoOpt.get();

        // Parse price from string
        BigDecimal price = BigDecimal.ZERO;
        if (info.getPriceMin() != null && !info.getPriceMin().isEmpty()) {
            try {
                price = new BigDecimal(info.getPriceMin());
            } catch (NumberFormatException e) {
                log.warn("Tui3GangAdapter: Failed to parse price: {}", info.getPriceMin());
            }
        }

        // Parse commission rates from API response
        BigDecimal sellerRate = parseRate(info.getSellerCommissionRate());
        BigDecimal shopeeRate = parseRate(info.getShopeeCommissionRate());

        // Calculate total commission rate: sellerRate + shopeeRate
        BigDecimal commissionRate = sellerRate.add(shopeeRate);

        // Calculate commission amount: commissionRate * price
        BigDecimal commission = commissionRate.multiply(price);

        ProductCommissionInfo commissionInfo = new ProductCommissionInfo(
            info.getProductName(),
            null,           // shopName not available in Tui3Gang API
            price,
            info.getImageUrl(),
            info.getProductLink(),
            sellerRate,     // NEW: hoa hồng từ seller (VD: 0.10 = 10%)
            shopeeRate,     // NEW: hoa hồng từ Shopee (VD: 0.05 = 5%)
            commission,     // commission = rate * price
            commissionRate, // total rate (VD: 0.15 = 15%)
            null,           // sales not available in Tui3Gang API
            false,          // isCapped - not provided by Tui3Gang
            null            // maxCap - not provided by Tui3Gang
        );

        log.info("Tui3GangAdapter: Product: {}, Price: {}, Rate: {}%, Commission: {}",
            commissionInfo.productName(), price, commissionRate.multiply(BigDecimal.valueOf(100)), commission);

        return Optional.of(commissionInfo);
    }

    /**
     * Parse commission rate from String to BigDecimal.
     * Returns ZERO if input is null, empty, or invalid.
     *
     * @param rateString Rate as string (e.g., "0.1" for 10%)
     * @return BigDecimal value or ZERO if parsing fails
     */
    private BigDecimal parseRate(String rateString) {
        if (rateString == null || rateString.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(rateString);
        } catch (NumberFormatException e) {
            log.warn("Tui3GangAdapter: Failed to parse rate: {}", rateString);
            return BigDecimal.ZERO;
        }
    }
}
