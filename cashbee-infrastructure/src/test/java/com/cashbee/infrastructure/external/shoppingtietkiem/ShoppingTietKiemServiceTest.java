package com.cashbee.infrastructure.external.shoppingtietkiem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingTietKiemServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ShoppingTietKiemAuthService authService;
    @Mock
    private ShoppingTietKiemProperties properties;

    @InjectMocks
    private ShoppingTietKiemService service;

    @Test
    void routesToNextAccountWhenProductInfoReturns401() {
        when(properties.getBaseUrl()).thenReturn("https://api.shoppingtietkiem.com");
        when(authService.getAccountCount()).thenReturn(3);
        when(authService.getAccessToken()).thenReturn(Optional.of("token-1"), Optional.of("token-2"));
        when(authService.buildHeaders(any())).thenReturn(new HttpHeaders());

        AtomicInteger calls = new AtomicInteger();
        when(restTemplate.exchange(
            eq("https://api.shoppingtietkiem.com/api/affiliate/product-info"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ShoppingTietKiemProductInfo.class)
        )).thenAnswer(inv -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                throw HttpClientErrorException.create(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    null,
                    "{}".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }
            ShoppingTietKiemProductInfo ok = ShoppingTietKiemProductInfo.builder()
                .productName("Product")
                .priceMin(new BigDecimal("10000"))
                .commissionRate(new BigDecimal("0.1"))
                .estimatedCashback(new BigDecimal("1000"))
                .build();
            return ResponseEntity.ok(ok);
        });

        Optional<ShoppingTietKiemProductInfo> result =
            service.getProductInfo("https://s.shopee.vn/60PrKrApTQ");

        assertTrue(result.isPresent());
        assertEquals("Product", result.get().getProductName());
        verify(authService).routeToNextAccount();
        verify(authService, atLeast(2)).getAccessToken();
    }
}
