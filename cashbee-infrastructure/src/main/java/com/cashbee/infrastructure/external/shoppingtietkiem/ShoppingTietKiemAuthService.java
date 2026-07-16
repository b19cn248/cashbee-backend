package com.cashbee.infrastructure.external.shoppingtietkiem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Authenticates against ShoppingTietKiem with multi-account fallback.
 *
 * Behaviour:
 * - Caches access token until near JWT expiry
 * - Tries accounts in order; if login fails, moves to the next account
 * - Remembers last successful account index for subsequent requests
 * - {@link #invalidateToken()} forces re-login (e.g. after HTTP 401 on product-info)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingTietKiemAuthService {

    private static final String LOGIN_PATH = "/api/auth/login";

    private final RestTemplate restTemplate;
    private final ShoppingTietKiemProperties properties;

    private final Object tokenLock = new Object();
    private final AtomicInteger accountCursor = new AtomicInteger(0);

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    /**
     * Returns a valid STK access token, logging in with account fallback if needed.
     *
     * @return access token, or empty if all accounts failed
     */
    public Optional<String> getAccessToken() {
        if (isTokenValid()) {
            return Optional.of(cachedAccessToken);
        }

        synchronized (tokenLock) {
            if (isTokenValid()) {
                return Optional.of(cachedAccessToken);
            }
            return loginWithFallback();
        }
    }

    /**
     * Clears cached token so the next call re-authenticates (optionally on next account).
     *
     * @param rotateAccount if true, advance account cursor before next login
     */
    public void invalidateToken(boolean rotateAccount) {
        synchronized (tokenLock) {
            cachedAccessToken = null;
            tokenExpiresAt = Instant.EPOCH;
            if (rotateAccount) {
                advanceAccountCursor();
            }
            log.info("ShoppingTietKiemAuth: Token invalidated (rotateAccount={})", rotateAccount);
        }
    }

    private Optional<String> loginWithFallback() {
        List<ShoppingTietKiemProperties.Account> accounts = properties.getAccounts();
        if (accounts == null || accounts.isEmpty()) {
            log.error("ShoppingTietKiemAuth: No STK accounts configured");
            return Optional.empty();
        }

        int size = accounts.size();
        int start = Math.floorMod(accountCursor.get(), size);

        for (int attempt = 0; attempt < size; attempt++) {
            int index = (start + attempt) % size;
            ShoppingTietKiemProperties.Account account = accounts.get(index);

            if (account == null
                || account.getEmail() == null || account.getEmail().isBlank()
                || account.getPassword() == null || account.getPassword().isBlank()) {
                log.warn("ShoppingTietKiemAuth: Skipping invalid account config at index {}", index);
                continue;
            }

            Optional<String> token = login(account.getEmail(), account.getPassword());
            if (token.isPresent()) {
                accountCursor.set(index);
                log.info("ShoppingTietKiemAuth: Login success with account index {} ({})",
                    index, maskEmail(account.getEmail()));
                return token;
            }

            log.warn("ShoppingTietKiemAuth: Login failed for account index {} ({}), trying next",
                index, maskEmail(account.getEmail()));
        }

        log.error("ShoppingTietKiemAuth: All {} STK accounts failed to login", size);
        return Optional.empty();
    }

    private Optional<String> login(String email, String password) {
        try {
            String apiUrl = properties.getBaseUrl() + LOGIN_PATH;

            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, buildHeaders(null));

            ResponseEntity<ShoppingTietKiemLoginResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                ShoppingTietKiemLoginResponse.class
            );

            ShoppingTietKiemLoginResponse loginResponse = response.getBody();
            if (response.getStatusCode().is2xxSuccessful()
                && loginResponse != null
                && loginResponse.getAccessToken() != null
                && !loginResponse.getAccessToken().isBlank()) {

                cachedAccessToken = loginResponse.getAccessToken();
                tokenExpiresAt = extractExpiry(cachedAccessToken)
                    .orElse(Instant.now().plusSeconds(14 * 60));
                return Optional.of(cachedAccessToken);
            }

            log.warn("ShoppingTietKiemAuth: Login non-success for {} - status={}, message={}",
                maskEmail(email),
                response.getStatusCode(),
                loginResponse != null ? loginResponse.getMessage() : null);
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("ShoppingTietKiemAuth: Login HTTP error for {}: {}", maskEmail(email), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("ShoppingTietKiemAuth: Unexpected login error for {}", maskEmail(email), e);
            return Optional.empty();
        }
    }

    private boolean isTokenValid() {
        if (cachedAccessToken == null || cachedAccessToken.isBlank()) {
            return false;
        }
        Instant threshold = Instant.now().plusSeconds(Math.max(0, properties.getTokenRefreshSkewSeconds()));
        return tokenExpiresAt != null && tokenExpiresAt.isAfter(threshold);
    }

    private void advanceAccountCursor() {
        List<ShoppingTietKiemProperties.Account> accounts = properties.getAccounts();
        if (accounts == null || accounts.isEmpty()) {
            return;
        }
        accountCursor.updateAndGet(i -> (i + 1) % accounts.size());
    }

    /**
     * Decode JWT payload {@code exp} without verifying signature (STK token is only used as bearer).
     */
    private Optional<Instant> extractExpiry(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return Optional.empty();
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(padBase64(parts[1])));
            // Minimal parse to avoid pulling in full JWT lib: look for "exp":NUMBER
            int expIdx = payloadJson.indexOf("\"exp\"");
            if (expIdx < 0) {
                return Optional.empty();
            }
            int colon = payloadJson.indexOf(':', expIdx);
            if (colon < 0) {
                return Optional.empty();
            }
            int i = colon + 1;
            while (i < payloadJson.length() && Character.isWhitespace(payloadJson.charAt(i))) {
                i++;
            }
            int start = i;
            while (i < payloadJson.length() && Character.isDigit(payloadJson.charAt(i))) {
                i++;
            }
            if (start == i) {
                return Optional.empty();
            }
            long exp = Long.parseLong(payloadJson.substring(start, i));
            return Optional.of(Instant.ofEpochSecond(exp));
        } catch (Exception e) {
            log.debug("ShoppingTietKiemAuth: Could not parse JWT exp: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String padBase64(String value) {
        int mod = value.length() % 4;
        if (mod == 0) {
            return value;
        }
        return value + "====".substring(mod);
    }

    HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set("x-api-key", properties.getApiKey());
        }
        if (properties.getAppPlatform() != null) {
            headers.set("x-app-platform", properties.getAppPlatform());
        }
        if (properties.getAppVersion() != null) {
            headers.set("x-app-version", properties.getAppVersion());
        }
        if (properties.getDeviceId() != null) {
            headers.set("x-device-id", properties.getDeviceId());
        }
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }
        return headers;
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }
}
