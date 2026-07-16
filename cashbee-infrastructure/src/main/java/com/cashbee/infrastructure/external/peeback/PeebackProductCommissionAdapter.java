package com.cashbee.infrastructure.external.peeback;

import com.cashbee.domain.service.ProductCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adapter implementing {@link ProductCommissionService} via Peeback API.
 *
 * Peeback returns seller/shopee rate split — best fit for CashBee estimate formula.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PeebackProductCommissionAdapter implements ProductCommissionService {

    private final PeebackService peebackService;

    @Override
    public Optional<ProductCommissionInfo> getProductCommission(String shopeeProductUrl) {
        log.info("PeebackAdapter: Getting commission for URL: {}", shopeeProductUrl);

        Optional<PeebackProductInfo> productInfoOpt = peebackService.getProductInfo(shopeeProductUrl);

        if (productInfoOpt.isEmpty()) {
            log.warn("PeebackAdapter: No product data for URL: {}", shopeeProductUrl);
            return Optional.empty();
        }

        PeebackProductInfo info = productInfoOpt.get();

        BigDecimal price = info.getPriceMin() != null ? info.getPriceMin() : BigDecimal.ZERO;
        BigDecimal sellerRate = info.getSellerCommissionRate();
        BigDecimal shopeeRate = info.getShopeeCommissionRate();
        BigDecimal commissionRate = info.getCommissionRate();

        if (commissionRate == null
            && sellerRate != null
            && shopeeRate != null) {
            commissionRate = sellerRate.add(shopeeRate);
        }
        if (commissionRate == null) {
            commissionRate = BigDecimal.ZERO;
        }

        BigDecimal commission = info.getCommission();
        if (commission == null && price.compareTo(BigDecimal.ZERO) > 0 && commissionRate != null) {
            commission = price.multiply(commissionRate);
        }

        String productLink = info.getProductLink();
        if (productLink == null || productLink.isBlank()) {
            if (info.getShopId() != null && info.getItemId() != null) {
                productLink = "https://shopee.vn/product/" + info.getShopId() + "/" + info.getItemId();
            } else {
                productLink = shopeeProductUrl;
            }
        }

        ProductCommissionInfo domainInfo = new ProductCommissionInfo(
            info.getProductName(),
            info.getShopName(),
            price,
            info.getImageUrl(),
            productLink,
            sellerRate,
            shopeeRate,
            commission,
            commissionRate,
            info.getSales(),
            false,
            null
        );

        log.info("PeebackAdapter: Product={}, Price={}, SellerRate={}, ShopeeRate={}, Commission={}",
            domainInfo.productName(),
            price,
            sellerRate,
            shopeeRate,
            commission);

        return Optional.of(domainInfo);
    }
}
