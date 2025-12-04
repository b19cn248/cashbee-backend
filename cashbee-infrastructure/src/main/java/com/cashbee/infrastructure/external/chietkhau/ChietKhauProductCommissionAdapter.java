package com.cashbee.infrastructure.external.chietkhau;

import com.cashbee.domain.service.ProductCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

        ProductCommissionInfo commissionInfo = new ProductCommissionInfo(
            info.getProductName(),
            info.getShopName(),
            new BigDecimal(info.getPrice()),
            info.getImageUrl(),
            info.getProductLink(),
            BigDecimal.valueOf(info.getCommission()),
            info.getSales(),  // Số lượt bán
            info.getIsLimitCap(),
            info.getCap() != null ? BigDecimal.valueOf(info.getCap()) : null
        );

        log.info("ChietKhauAdapter: Converted commission info - Product: {}, Commission: {}, Sales: {}",
            commissionInfo.productName(), commissionInfo.commission(), commissionInfo.sales());

        return Optional.of(commissionInfo);
    }
}
