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
import java.util.concurrent.atomic.AtomicInteger;

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

        ShoppingTietKiemProperties.Account a3 = new ShoppingTietKiemProperties.Account();
        a3.setEmail("a3@example.com");
        a3.setPassword("also-good");

        properties.setAccounts(List.of(a1, a2, a3));
        authService = new ShoppingTietKiemAuthService(restTemplate, properties);
    }

    @Test
    void routesAcc1FailToAcc2OnLogin() {
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
            // acc2 / acc3 succeed
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
        // acc1 fail + acc2 success = 2 login calls
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

    @Test
    void routesAcc1AndAcc2FailToAcc3() {
        AtomicInteger loginCalls = new AtomicInteger();
        when(restTemplate.exchange(
            eq("https://api.shoppingtietkiem.com/api/auth/login"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ShoppingTietKiemLoginResponse.class)
        )).thenAnswer(invocation -> {
            loginCalls.incrementAndGet();
            HttpEntity<?> entity = invocation.getArgument(2);
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> body = (java.util.Map<String, String>) entity.getBody();
            String email = body.get("email");
            if ("a3@example.com".equals(email)) {
                long exp = (System.currentTimeMillis() / 1000L) + 900;
                return ResponseEntity.ok(ShoppingTietKiemLoginResponse.builder()
                    .accessToken(fakeJwt(exp))
                    .message("ok")
                    .build());
            }
            throw HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                null,
                "{\"message\":\"bad\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
            );
        });

        Optional<String> token = authService.getAccessToken();

        assertTrue(token.isPresent());
        // acc1 fail + acc2 fail + acc3 ok
        assertEquals(3, loginCalls.get());
    }

    @Test
    void routeToNextAccountAdvancesForProductInfoRetry() {
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
            // only acc2 works
            if (!"a2@example.com".equals(email)) {
                throw HttpClientErrorException.create(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    null,
                    "{}".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }
            long exp = (System.currentTimeMillis() / 1000L) + 900;
            return ResponseEntity.ok(ShoppingTietKiemLoginResponse.builder()
                .accessToken(fakeJwt(exp))
                .message("ok")
                .build());
        });

        // First getAccessToken: acc1 fail → acc2 ok
        assertTrue(authService.getAccessToken().isPresent());

        // Simulate product-info 401 → route to acc3
        authService.routeToNextAccount();

        // Next login starts at acc3 (fails) then wraps? After success we were on acc2 (index 1).
        // routeToNext → index 2 = acc3. acc3 fails, then (start+1)%3 = acc1 fails, then acc2 ok.
        assertTrue(authService.getAccessToken().isPresent());
        assertEquals(3, authService.getAccountCount());
    }

    private static String fakeJwt(long expEpochSeconds) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"exp\":" + expEpochSeconds + "}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }
}
