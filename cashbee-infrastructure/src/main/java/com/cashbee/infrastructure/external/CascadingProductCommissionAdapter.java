package com.cashbee.infrastructure.external;

import com.cashbee.domain.service.ProductCommissionService;
import com.cashbee.infrastructure.external.chietkhau.ChietKhauProductCommissionAdapter;
import com.cashbee.infrastructure.external.peeback.PeebackProductCommissionAdapter;
import com.cashbee.infrastructure.external.shoppingtietkiem.ShoppingTietKiemProductCommissionAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Cascading product-commission source for estimate-cashback.
 *
 * Order (most reliable first):
 * 1. Peeback — public API, no login, returns seller/shopee rate split
 * 2. ShoppingTietKiem — multi-account auth (can fail on credentials)
 * 3. ChietKhau.Pro — last resort (no rate split)
 */
@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class CascadingProductCommissionAdapter implements ProductCommissionService {

    private final PeebackProductCommissionAdapter peebackAdapter;
    private final ShoppingTietKiemProductCommissionAdapter shoppingTietKiemAdapter;
    private final ChietKhauProductCommissionAdapter chietKhauAdapter;

    @Override
    public Optional<ProductCommissionInfo> getProductCommission(String shopeeProductUrl) {
        log.info("CascadingCommission: Resolving commission for URL: {}", shopeeProductUrl);

        Optional<ProductCommissionInfo> peeback = peebackAdapter.getProductCommission(shopeeProductUrl);
        if (peeback.isPresent()) {
            log.info("CascadingCommission: Source=Peeback");
            return peeback;
        }

        Optional<ProductCommissionInfo> stk = shoppingTietKiemAdapter.getProductCommission(shopeeProductUrl);
        if (stk.isPresent()) {
            log.info("CascadingCommission: Source=ShoppingTietKiem");
            return stk;
        }

        Optional<ProductCommissionInfo> chietKhau = chietKhauAdapter.getProductCommission(shopeeProductUrl);
        if (chietKhau.isPresent()) {
            log.info("CascadingCommission: Source=ChietKhau");
            return chietKhau;
        }

        log.warn("CascadingCommission: All sources failed for URL: {}", shopeeProductUrl);
        return Optional.empty();
    }
}
