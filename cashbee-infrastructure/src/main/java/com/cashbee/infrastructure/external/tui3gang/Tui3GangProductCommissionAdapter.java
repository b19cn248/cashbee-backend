package com.cashbee.infrastructure.external.tui3gang;

import com.cashbee.domain.service.ProductCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
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
 * IMPORTANT: Tui3Gang API returns commission as 60% of full commission.
 * This adapter passes that value directly - the UseCase will recalculate for 80%.
 *
 * @Primary annotation makes this adapter the default choice when
 * ProductCommissionService is injected (replacing ChietKhauProductCommissionAdapter).
 *
 * @author CashBee Team
 */
@Component
@Primary
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

        // Commission from Tui3Gang is 60% of full commission
        // We pass it directly - UseCase will recalculate for 80%
        BigDecimal commission = info.getCommission() != null
            ? BigDecimal.valueOf(info.getCommission())
            : BigDecimal.ZERO;

        ProductCommissionInfo commissionInfo = new ProductCommissionInfo(
            info.getProductName(),
            null,  // shopName not available in Tui3Gang API
            price,
            info.getImageUrl(),
            info.getProductLink(),
            commission,  // 60% commission - UseCase will convert to 80%
            null,        // sales not available in Tui3Gang API
            false,       // isCapped - not provided by Tui3Gang
            null         // maxCap - not provided by Tui3Gang
        );

        log.info("Tui3GangAdapter: Converted commission info - Product: {}, Price: {}, Commission (60%): {}",
            commissionInfo.productName(), commissionInfo.price(), commissionInfo.commission());

        return Optional.of(commissionInfo);
    }
}
