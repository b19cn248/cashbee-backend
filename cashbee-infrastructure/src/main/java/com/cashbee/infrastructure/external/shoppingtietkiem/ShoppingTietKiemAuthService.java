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
 * STK multi-account auth with sequential routing.
 *
 * Routing rule (không "chọn" account cứng):
 * <pre>
 *   acc1 fail → route acc2
 *   acc2 fail → route acc3
 *   ...
 *   hết list  → empty
 * </pre>
 *
 * - Token cache theo account đang active
 * - Login fail: tự route sang account kế tiếp trong cùng lần gọi
 * - Product-info 401/403: {@link #routeToNextAccount()} rồi login lại
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingTietKiemAuthService {

    private static final String LOGIN_PATH = "/api/auth/login";

    private final RestTemplate restTemplate;
    private final ShoppingTietKiemProperties properties;

    private final Object tokenLock = new Object();

    /**
     * Index of the account currently preferred for login (0-based).
     * Starts at 0 = acc1; advances only on route after failure.
     */
    private final AtomicInteger activeAccountIndex = new AtomicInteger(0);

    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;
    private volatile int tokenAccountIndex = -1;

    /**
     * Number of configured STK accounts (for retry bounds in product-info).
     */
    public int getAccountCount() {
        List<ShoppingTietKiemProperties.Account> accounts = properties.getAccounts();
        return accounts == null ? 0 : accounts.size();
    }

    /**
     * Returns a valid STK access token.
     * <p>
     * If cached token is still valid → reuse.
     * Else login starting from {@link #activeAccountIndex}; on fail route to next account
     * until one succeeds or all accounts are exhausted.
     */
    public Optional<String> getAccessToken() {
        if (isTokenValid()) {
            return Optional.of(cachedAccessToken);
        }

        synchronized (tokenLock) {
            if (isTokenValid()) {
                return Optional.of(cachedAccessToken);
            }
            return loginWithAccountRouting();
        }
    }

    /**
     * Force re-login with the next account (after product-info 401/403).
     * <p>
     * Clears cache and advances active index: acc1 → acc2 → acc3 → acc1 ...
     */
    public void routeToNextAccount() {
        synchronized (tokenLock) {
            clearTokenCache();
            int size = getAccountCount();
            if (size <= 0) {
                return;
            }
            int from = Math.floorMod(activeAccountIndex.get(), size);
            int to = (from + 1) % size;
            activeAccountIndex.set(to);
            log.info("ShoppingTietKiemAuth: Route account {} → {} (acc{} → acc{})",
                from, to, from + 1, to + 1);
        }
    }

    /**
     * Clears cached token without changing active account (e.g. expiry refresh).
     */
    public void invalidateToken() {
        synchronized (tokenLock) {
            clearTokenCache();
            log.info("ShoppingTietKiemAuth: Token invalidated (same account)");
        }
    }

    /**
     * @param rotateAccount if true, same as {@link #routeToNextAccount()}; else {@link #invalidateToken()}
     * @deprecated Prefer {@link #routeToNextAccount()} for explicit routing semantics
     */
    @Deprecated
    public void invalidateToken(boolean rotateAccount) {
        if (rotateAccount) {
            routeToNextAccount();
        } else {
            invalidateToken();
        }
    }

    /**
     * Try login from active account, then route sequentially through the full list once.
     *
     * Example with 3 accounts, active=0:
     * try acc1 → fail → try acc2 → fail → try acc3 → fail → empty
     */
    private Optional<String> loginWithAccountRouting() {
        List<ShoppingTietKiemProperties.Account> accounts = properties.getAccounts();
        if (accounts == null || accounts.isEmpty()) {
            log.error("ShoppingTietKiemAuth: No STK accounts configured");
            return Optional.empty();
        }

        int size = accounts.size();
        int start = Math.floorMod(activeAccountIndex.get(), size);

        log.info("ShoppingTietKiemAuth: Login routing starts at acc{} (index {}), total={}",
            start + 1, start, size);

        for (int offset = 0; offset < size; offset++) {
            int index = (start + offset) % size;
            ShoppingTietKiemProperties.Account account = accounts.get(index);

            if (!isAccountConfigured(account)) {
                log.warn("ShoppingTietKiemAuth: acc{} (index {}) not configured — route next",
                    index + 1, index);
                continue;
            }

            log.info("ShoppingTietKiemAuth: Trying login acc{} ({})",
                index + 1, maskEmail(account.getEmail()));

            Optional<String> token = login(account.getEmail(), account.getPassword());
            if (token.isPresent()) {
                activeAccountIndex.set(index);
                tokenAccountIndex = index;
                log.info("ShoppingTietKiemAuth: Login OK on acc{} ({})",
                    index + 1, maskEmail(account.getEmail()));
                return token;
            }

            int next = (index + 1) % size;
            if (offset < size - 1) {
                log.warn("ShoppingTietKiemAuth: Login FAILED acc{} ({}) — route → acc{}",
                    index + 1, maskEmail(account.getEmail()), next + 1);
            } else {
                log.warn("ShoppingTietKiemAuth: Login FAILED acc{} ({}) — no more accounts",
                    index + 1, maskEmail(account.getEmail()));
            }
        }

        log.error("ShoppingTietKiemAuth: All {} STK accounts failed login routing", size);
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

    private void clearTokenCache() {
        cachedAccessToken = null;
        tokenExpiresAt = Instant.EPOCH;
        tokenAccountIndex = -1;
    }

    private boolean isTokenValid() {
        if (cachedAccessToken == null || cachedAccessToken.isBlank()) {
            return false;
        }
        Instant threshold = Instant.now().plusSeconds(Math.max(0, properties.getTokenRefreshSkewSeconds()));
        return tokenExpiresAt != null && tokenExpiresAt.isAfter(threshold);
    }

    private static boolean isAccountConfigured(ShoppingTietKiemProperties.Account account) {
        return account != null
            && account.getEmail() != null && !account.getEmail().isBlank()
            && account.getPassword() != null && !account.getPassword().isBlank();
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
