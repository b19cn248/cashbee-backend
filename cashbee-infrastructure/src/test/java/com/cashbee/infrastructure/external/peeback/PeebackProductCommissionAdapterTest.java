package com.cashbee.infrastructure.external.peeback;

import com.cashbee.domain.service.ProductCommissionService.ProductCommissionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeebackProductCommissionAdapterTest {

    @Mock
    private PeebackService peebackService;

    @InjectMocks
    private PeebackProductCommissionAdapter adapter;

    @Test
    void mapsPeebackResponseWithSellerAndShopeeRates() {
        PeebackProductInfo pb = PeebackProductInfo.builder()
            .productName("Túi đựng rác")
            .shopId(528677227L)
            .itemId(25892454268L)
            .priceMin(new BigDecimal("76740"))
            .imageUrl("https://cf.shopee.vn/file/x")
            .commissionRate(new BigDecimal("0.115"))
            .sellerCommissionRate(new BigDecimal("0.0800"))
            .shopeeCommissionRate(new BigDecimal("0.0350"))
            .commission(new BigDecimal("8825"))
            .build();

        when(peebackService.getProductInfo(anyString())).thenReturn(Optional.of(pb));

        Optional<ProductCommissionInfo> result =
            adapter.getProductCommission("https://s.shopee.vn/60PrKrApTQ");

        assertTrue(result.isPresent());
        ProductCommissionInfo info = result.get();
        assertEquals("Túi đựng rác", info.productName());
        assertEquals(new BigDecimal("76740"), info.price());
        assertEquals(new BigDecimal("0.0800"), info.sellerCommissionRate());
        assertEquals(new BigDecimal("0.0350"), info.shopeeCommissionRate());
        assertEquals(new BigDecimal("0.115"), info.commissionRate());
        assertEquals(new BigDecimal("8825"), info.commission());
        assertEquals("https://shopee.vn/product/528677227/25892454268", info.productLink());
    }

    @Test
    void returnsEmptyWhenPeebackFails() {
        when(peebackService.getProductInfo(anyString())).thenReturn(Optional.empty());

        Optional<ProductCommissionInfo> result =
            adapter.getProductCommission("https://s.shopee.vn/60PrKrApTQ");

        assertTrue(result.isEmpty());
    }
}
