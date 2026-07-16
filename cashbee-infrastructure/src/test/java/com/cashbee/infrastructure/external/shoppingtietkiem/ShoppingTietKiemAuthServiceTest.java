package com.cashbee.infrastructure.external.shoppingtietkiem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingTietKiemAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ShoppingTietKiemProperties properties;
    private ShoppingTietKiemAuthService authService;

    @BeforeEach
    void setUp() {
        properties = new ShoppingTietKiemProperties();
        properties.setBaseUrl("https://api.shoppingtietkiem.com");
        properties.setApiKey("test-key");
        properties.setTokenRefreshSkewSeconds(60);

        ShoppingTietKiemProperties.Account a1 = new ShoppingTietKiemProperties.Account();
        a1.setEmail("a1@example.com");
        a1.setPassword("bad");

        ShoppingTietKiemProperties.Account a2 = new ShoppingTietKiemProperties.Account();
        a2.setEmail("a2@example.com");
        a2.setPassword("good");

        properties.setAccounts(List.of(a1, a2));
        authService = new ShoppingTietKiemAuthService(restTemplate, properties);
    }

    @Test
    void fallsBackToNextAccountWhenFirstLoginFails() {
        when(restTemplate.exchange(
            eq("https://api.shoppingtietkiem.com/api/auth/login"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ShoppingTietKiemLoginResponse.class)
        )).thenAnswer(invocation -> {
            HttpEntity<?> entity = invocation.getArgument(2);
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> body = (java.util.Map<String, String>) entity.getBody();
            String email = body.get("email");
            if ("a1@example.com".equals(email)) {
                throw HttpClientErrorException.create(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    null,
                    "{\"message\":\"bad\"}".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }
            long exp = (System.currentTimeMillis() / 1000L) + 900;
            String token = fakeJwt(exp);
            ShoppingTietKiemLoginResponse ok = ShoppingTietKiemLoginResponse.builder()
                .accessToken(token)
                .message("ok")
                .build();
            return ResponseEntity.ok(ok);
        });

        Optional<String> token = authService.getAccessToken();

        assertTrue(token.isPresent());
        verify(restTemplate, times(2)).exchange(
            eq("https://api.shoppingtietkiem.com/api/auth/login"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ShoppingTietKiemLoginResponse.class)
        );

        // second call should reuse cached token
        Optional<String> cached = authService.getAccessToken();
        assertEquals(token.get(), cached.orElse(null));
        verify(restTemplate, times(2)).exchange(
            ArgumentMatchers.anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ShoppingTietKiemLoginResponse.class)
        );
    }

    private static String fakeJwt(long expEpochSeconds) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"exp\":" + expEpochSeconds + "}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }
}
