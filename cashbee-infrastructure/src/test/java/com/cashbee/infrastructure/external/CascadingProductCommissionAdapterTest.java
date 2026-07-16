package com.cashbee.infrastructure.external;

import com.cashbee.domain.service.ProductCommissionService.ProductCommissionInfo;
import com.cashbee.infrastructure.external.chietkhau.ChietKhauProductCommissionAdapter;
import com.cashbee.infrastructure.external.peeback.PeebackProductCommissionAdapter;
import com.cashbee.infrastructure.external.shoppingtietkiem.ShoppingTietKiemProductCommissionAdapter;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CascadingProductCommissionAdapterTest {

    @Mock
    private PeebackProductCommissionAdapter peebackAdapter;
    @Mock
    private ShoppingTietKiemProductCommissionAdapter shoppingTietKiemAdapter;
    @Mock
    private ChietKhauProductCommissionAdapter chietKhauAdapter;

    @InjectMocks
    private CascadingProductCommissionAdapter cascading;

    private ProductCommissionInfo sample(String name) {
        return new ProductCommissionInfo(
            name, "shop", new BigDecimal("1000"), "img", "link",
            new BigDecimal("0.08"), new BigDecimal("0.03"),
            new BigDecimal("110"), new BigDecimal("0.11"),
            10, false, null
        );
    }

    @Test
    void usesPeebackWhenAvailable() {
        when(peebackAdapter.getProductCommission(anyString()))
            .thenReturn(Optional.of(sample("from-peeback")));

        Optional<ProductCommissionInfo> result =
            cascading.getProductCommission("https://s.shopee.vn/60PrKrApTQ");

        assertTrue(result.isPresent());
        assertEquals("from-peeback", result.get().productName());
        verify(shoppingTietKiemAdapter, never()).getProductCommission(anyString());
        verify(chietKhauAdapter, never()).getProductCommission(anyString());
    }

    @Test
    void fallsBackToStkWhenPeebackFails() {
        when(peebackAdapter.getProductCommission(anyString())).thenReturn(Optional.empty());
        when(shoppingTietKiemAdapter.getProductCommission(anyString()))
            .thenReturn(Optional.of(sample("from-stk")));

        Optional<ProductCommissionInfo> result =
            cascading.getProductCommission("https://s.shopee.vn/60PrKrApTQ");

        assertTrue(result.isPresent());
        assertEquals("from-stk", result.get().productName());
        verify(chietKhauAdapter, never()).getProductCommission(anyString());
    }

    @Test
    void fallsBackToChietKhauWhenPeebackAndStkFail() {
        when(peebackAdapter.getProductCommission(anyString())).thenReturn(Optional.empty());
        when(shoppingTietKiemAdapter.getProductCommission(anyString())).thenReturn(Optional.empty());
        when(chietKhauAdapter.getProductCommission(anyString()))
            .thenReturn(Optional.of(sample("from-chietkhau")));

        Optional<ProductCommissionInfo> result =
            cascading.getProductCommission("https://s.shopee.vn/60PrKrApTQ");

        assertTrue(result.isPresent());
        assertEquals("from-chietkhau", result.get().productName());
    }

    @Test
    void returnsEmptyWhenAllSourcesFail() {
        when(peebackAdapter.getProductCommission(anyString())).thenReturn(Optional.empty());
        when(shoppingTietKiemAdapter.getProductCommission(anyString())).thenReturn(Optional.empty());
        when(chietKhauAdapter.getProductCommission(anyString())).thenReturn(Optional.empty());

        Optional<ProductCommissionInfo> result =
            cascading.getProductCommission("https://s.shopee.vn/60PrKrApTQ");

        assertTrue(result.isEmpty());
    }
}
