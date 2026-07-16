package com.cashbee.infrastructure.external.shoppingtietkiem;

import com.cashbee.domain.service.ProductCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adapter implementing {@link ProductCommissionService} via ShoppingTietKiem API.
 *
 * Maps STK product-info into domain {@link ProductCommissionInfo} so
 * {@code EstimateCashbackUseCase} (and CashBee API response) stay unchanged.
 *
 * STK only returns a total commission rate (no seller/shopee split). Mapping:
 * - sellerCommissionRate = STK commissionRate (no cap applied in use case)
 * - shopeeCommissionRate = ZERO (avoids incorrect 50k shopee cap on total rate)
 * - commission = STK estimatedCashback
 * - commissionRate = STK commissionRate
 *
 * Used as fallback #2 by {@code CascadingProductCommissionAdapter} (after Peeback).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShoppingTietKiemProductCommissionAdapter implements ProductCommissionService {

    private final ShoppingTietKiemService shoppingTietKiemService;

    @Override
    public Optional<ProductCommissionInfo> getProductCommission(String shopeeProductUrl) {
        log.info("ShoppingTietKiemAdapter: Getting commission for URL: {}", shopeeProductUrl);

        Optional<ShoppingTietKiemProductInfo> productInfoOpt =
            shoppingTietKiemService.getProductInfo(shopeeProductUrl);

        if (productInfoOpt.isEmpty()) {
            log.warn("ShoppingTietKiemAdapter: No product data for URL: {}", shopeeProductUrl);
            return Optional.empty();
        }

        ShoppingTietKiemProductInfo info = productInfoOpt.get();

        BigDecimal price = info.getPriceMin() != null ? info.getPriceMin() : BigDecimal.ZERO;
        BigDecimal commissionRate = info.getCommissionRate() != null
            ? info.getCommissionRate()
            : BigDecimal.ZERO;
        BigDecimal sellerRate = commissionRate;
        BigDecimal shopeeRate = BigDecimal.ZERO;

        BigDecimal commission = info.getEstimatedCashback();
        if (commission == null) {
            commission = price.multiply(commissionRate);
        }

        ProductCommissionInfo domainInfo = new ProductCommissionInfo(
            info.getProductName(),
            info.getShopName(),
            price,
            info.getImageUrl(),
            info.getProductLink() != null ? info.getProductLink() : shopeeProductUrl,
            sellerRate,
            shopeeRate,
            commission,
            commissionRate,
            info.getSales(),
            false,
            null
        );

        log.info("ShoppingTietKiemAdapter: Product={}, Price={}, Rate={}%, Commission={}",
            domainInfo.productName(),
            price,
            commissionRate.multiply(BigDecimal.valueOf(100)),
            commission);

        return Optional.of(domainInfo);
    }
}
