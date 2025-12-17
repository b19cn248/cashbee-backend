package com.cashbee.infrastructure.external.chietkhau;

import com.cashbee.domain.service.ProductCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Adapter that implements ProductCommissionService using ChietKhau.Pro API.
 *
 * This is the ADAPTER in Hexagonal Architecture:
 * - Implements the PORT (ProductCommissionService interface from Domain)
 * - Uses ChietKhauProService to call external API
 * - Converts external DTOs to domain DTOs
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChietKhauProductCommissionAdapter implements ProductCommissionService {

    private final ChietKhauProService chietKhauProService;

    @Override
    public Optional<ProductCommissionInfo> getProductCommission(String shopeeProductUrl) {
        log.info("ChietKhauAdapter: Getting commission for URL: {}", shopeeProductUrl);

        // Call ChietKhau.Pro API
        Optional<ChietKhauProductInfo> productInfoOpt = chietKhauProService.getProductCommission(shopeeProductUrl);

        if (productInfoOpt.isEmpty()) {
            log.warn("ChietKhauAdapter: No commission data returned for URL: {}", shopeeProductUrl);
            return Optional.empty();
        }

        // Convert to domain DTO
        ChietKhauProductInfo info = productInfoOpt.get();

        BigDecimal price = new BigDecimal(info.getPrice());
        BigDecimal commission = BigDecimal.valueOf(info.getCommission());

        // Calculate commission rate: commission / price
        BigDecimal commissionRate = BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            commissionRate = commission.divide(price, 4, RoundingMode.HALF_UP);
        }

        ProductCommissionInfo commissionInfo = new ProductCommissionInfo(
            info.getProductName(),
            info.getShopName(),
            price,
            info.getImageUrl(),
            info.getProductLink(),
            commission,
            commissionRate,   // commission / price
            info.getSales(),  // Số lượt bán
            info.getIsLimitCap(),
            info.getCap() != null ? BigDecimal.valueOf(info.getCap()) : null
        );

        log.info("ChietKhauAdapter: Product: {}, Price: {}, Rate: {}%, Commission: {}",
            commissionInfo.productName(), price, commissionRate.multiply(BigDecimal.valueOf(100)), commission);

        return Optional.of(commissionInfo);
    }
}
