package com.cashbee.application.util.affiliate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Service to expand Shopee shortened links (s.shopee.vn) to full product URLs.
 *
 * Shopee shortened links format: https://s.shopee.vn/{short_code}
 * Example: https://s.shopee.vn/12Y5L6SJB
 *
 * This service follows HTTP redirects to get the final product URL,
 * which can then be parsed normally by ShopeeUrlParser.
 *
 * Features:
 * - Follow redirect chain (HTTP 301/302/303/307/308)
 * - Timeout protection (5 seconds)
 * - Redirect loop detection (max 5 redirects)
 * - Uses HEAD request for performance (no body download)
 *
 * @author CashBee Team
 */
@Component
@Slf4j
public class ShopeeUrlExpanderService {

    /**
     * Maximum number of redirects to follow.
     * Prevents infinite redirect loops.
     */
    private static final int MAX_REDIRECTS = 5;

    /**
     * Connection timeout in milliseconds (5 seconds).
     * Prevents hanging on slow networks.
     */
    private static final int TIMEOUT_MS = 5000;

    /**
     * Expand a shortened Shopee URL to its final destination.
     *
     * This method follows HTTP redirects until it reaches the final URL.
     * It handles redirect codes: 301, 302, 303, 307, 308.
     *
     * @param shortenedUrl Shortened URL (e.g., https://s.shopee.vn/12Y5L6SJB)
     * @return Final expanded URL (e.g., https://shopee.vn/Product-i.123.456)
     * @throws IllegalArgumentException if URL is invalid, redirect loop, timeout, or too many redirects
     */
    public String expandUrl(String shortenedUrl) {
        log.info("Expanding shortened URL: {}", shortenedUrl);

        // Track visited URLs to detect redirect loops
        Set<String> visitedUrls = new HashSet<>();
        String currentUrl = shortenedUrl;
        int redirectCount = 0;

        try {
            while (redirectCount < MAX_REDIRECTS) {
                // Check for redirect loop
                if (visitedUrls.contains(currentUrl)) {
                    log.error("Redirect loop detected at URL: {}", currentUrl);
                    throw new IllegalArgumentException("Redirect loop detected when expanding URL");
                }
                visitedUrls.add(currentUrl);

                log.debug("Following redirect #{}: {}", redirectCount + 1, currentUrl);

                // Create connection
                URL url = new URL(currentUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                try {
                    // Use HEAD request for performance (don't download body)
                    connection.setRequestMethod("HEAD");
                    connection.setInstanceFollowRedirects(false);  // Manual redirect handling
                    connection.setConnectTimeout(TIMEOUT_MS);
                    connection.setReadTimeout(TIMEOUT_MS);

                    // Set User-Agent to avoid being blocked
                    connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

                    // Get response code
                    int responseCode = connection.getResponseCode();
                    log.debug("Response code: {}", responseCode);

                    // Check if this is a redirect
                    if (isRedirect(responseCode)) {
                        String location = connection.getHeaderField("Location");

                        if (location == null || location.isBlank()) {
                            log.error("Redirect response but no Location header at URL: {}", currentUrl);
                            throw new IllegalArgumentException("Invalid redirect: no Location header");
                        }

                        // Handle relative URLs
                        if (location.startsWith("/")) {
                            String protocol = url.getProtocol();
                            String host = url.getHost();
                            location = protocol + "://" + host + location;
                            log.debug("Converted relative URL to absolute: {}", location);
                        }

                        log.info("Redirect #{}: {} -> {}", redirectCount + 1, currentUrl, location);
                        currentUrl = location;
                        redirectCount++;
                    } else if (responseCode >= 200 && responseCode < 300) {
                        // Success - reached final URL
                        log.info("Successfully expanded URL after {} redirects: {} -> {}",
                            redirectCount, shortenedUrl, currentUrl);
                        return currentUrl;
                    } else {
                        // Unexpected response code
                        log.error("Unexpected HTTP response code {} for URL: {}", responseCode, currentUrl);
                        throw new IllegalArgumentException(
                            "Failed to expand URL: HTTP " + responseCode);
                    }
                } finally {
                    connection.disconnect();
                }
            }

            // Too many redirects
            log.error("Too many redirects (max {}): {}", MAX_REDIRECTS, shortenedUrl);
            throw new IllegalArgumentException(
                "Too many redirects (max " + MAX_REDIRECTS + ") when expanding URL");

        } catch (IOException e) {
            log.error("Network error while expanding URL: {}", shortenedUrl, e);
            throw new IllegalArgumentException(
                "Failed to expand shortened link: " + e.getMessage(), e);
        }
    }

    /**
     * Check if HTTP response code indicates a redirect.
     *
     * Redirect codes:
     * - 301: Moved Permanently
     * - 302: Found (Temporary Redirect)
     * - 303: See Other
     * - 307: Temporary Redirect
     * - 308: Permanent Redirect
     *
     * @param responseCode HTTP response code
     * @return true if this is a redirect code
     */
    private boolean isRedirect(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM    // 301
            || responseCode == HttpURLConnection.HTTP_MOVED_TEMP    // 302
            || responseCode == HttpURLConnection.HTTP_SEE_OTHER     // 303
            || responseCode == 307  // Temporary Redirect
            || responseCode == 308; // Permanent Redirect
    }

    /**
     * Check if a URL is a Shopee shortened link.
     *
     * @param url URL to check
     * @return true if this is a shortened link (s.shopee.vn or s.shopee.{country})
     */
    public boolean isShortenedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        // Match s.shopee.vn, s.shopee.sg, s.shopee.my, etc.
        return url.matches("https?://s\\.shopee\\.[a-z]{2,3}/.*");
    }
}
