package com.cashbee.infrastructure.external.shoppingtietkiem;

import com.cashbee.domain.service.ProductCommissionService.ProductCommissionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingTietKiemProductCommissionAdapterTest {

    @Mock
    private ShoppingTietKiemService shoppingTietKiemService;

    @InjectMocks
    private ShoppingTietKiemProductCommissionAdapter adapter;

    @Test
    void mapsStkResponseToDomainWithoutChangingCashBeeContract() {
        ShoppingTietKiemProductInfo stk = ShoppingTietKiemProductInfo.builder()
            .productName("Doraemon")
            .shopName("HDRBOOKS")
            .priceMin(new BigDecimal("345000"))
            .imageUrl("https://img.example/1.jpg")
            .productLink("https://shopee.vn/product/1/2")
            .commissionRate(new BigDecimal("0.035"))
            .estimatedCashback(new BigDecimal("12075"))
            .sales(44)
            .build();

        when(shoppingTietKiemService.getProductInfo(anyString())).thenReturn(Optional.of(stk));

        Optional<ProductCommissionInfo> result =
            adapter.getProductCommission("https://s.shopee.vn/8fQcjUMERn");

        assertTrue(result.isPresent());
        ProductCommissionInfo info = result.get();
        assertEquals("Doraemon", info.productName());
        assertEquals("HDRBOOKS", info.shopName());
        assertEquals(new BigDecimal("345000"), info.price());
        assertEquals(new BigDecimal("0.035"), info.sellerCommissionRate());
        assertEquals(BigDecimal.ZERO, info.shopeeCommissionRate());
        assertEquals(new BigDecimal("0.035"), info.commissionRate());
        assertEquals(new BigDecimal("12075"), info.commission());
        assertEquals(44, info.sales());
        assertFalse(info.isCapped());
    }

    @Test
    void returnsEmptyWhenStkFails() {
        when(shoppingTietKiemService.getProductInfo(anyString())).thenReturn(Optional.empty());

        Optional<ProductCommissionInfo> result =
            adapter.getProductCommission("https://s.shopee.vn/8fQcjUMERn");

        assertTrue(result.isEmpty());
    }
}
