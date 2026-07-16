package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.EstimateCashbackRequest;
import com.cashbee.application.dto.affiliate.EstimateCashbackResponse;
import com.cashbee.application.util.affiliate.ShopeeUrlParser;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.service.ProductCommissionService;
import com.cashbee.domain.service.ProductCommissionService.ProductCommissionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Use case for estimating cashback amount for a Shopee product.
 *
 * This use case:
 * 1. Validates and parses the Shopee URL
 * 2. Calls external API (via ProductCommissionService port) to get commission data
 * 3. Returns estimated cashback with product details
 *
 * Formula:
 * - shopeeCommission = min(price * shopeeRate, 50000)  // Capped at 50k
 * - sellerCommission = price * sellerRate              // No cap
 * - estimatedCashback = shopeeCommission + sellerCommission
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstimateCashbackUseCase {

    private final ProductCommissionService productCommissionService;
    private final ShopeeUrlParser shopeeUrlParser;

    /**
     * Number formatter for Vietnamese currency.
     */
    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    /**
     * Maximum commission from Shopee (50,000 VND).
     */
    private static final BigDecimal SHOPEE_COMMISSION_CAP = new BigDecimal("50000");

    /**
     * Execute the use case to estimate cashback.
     *
     * @param request Request containing Shopee URL
     * @return Response with estimated cashback and product details
     * @throws BusinessException if URL is invalid or API call fails
     */
    public EstimateCashbackResponse execute(EstimateCashbackRequest request) {
        log.info("EstimateCashbackUseCase: Estimating cashback for URL: {}", request.getShopeeUrl());

        // Step 1: Validate URL. Prefer clean product URL when parse succeeds.
        // If expand/parse fails (Shopee block, timeout) but URL still looks like Shopee,
        // continue with original — Peeback/STK accept short links directly.
        String originalUrl = request.getShopeeUrl();
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new BusinessException("Invalid Shopee URL: empty");
        }
        String productUrl = originalUrl.trim();
        try {
            ShopeeUrlParser.ParsedShopeeUrl parsedUrl = shopeeUrlParser.parse(productUrl);
            if (parsedUrl.hasShopId() && parsedUrl.getItemId() != null && !parsedUrl.getItemId().isBlank()) {
                productUrl = "https://shopee.vn/product/" + parsedUrl.getShopId() + "/" + parsedUrl.getItemId();
            }
            log.info("EstimateCashbackUseCase: Parsed URL - Shop ID: {}, Item ID: {}, commissionUrl={}",
                parsedUrl.getShopId(), parsedUrl.getItemId(), productUrl);
        } catch (IllegalArgumentException e) {
            if (looksLikeShopeeUrl(originalUrl)) {
                log.warn("EstimateCashbackUseCase: Parse/expand failed, using original URL. cause={}",
                    e.getMessage());
                productUrl = originalUrl.trim();
            } else {
                log.error("EstimateCashbackUseCase: Invalid Shopee URL: {}", originalUrl, e);
                throw new BusinessException("Invalid Shopee URL: " + e.getMessage());
            }
        }

        // Step 2: Call external API via port (cascade: Peeback → STK → ChietKhau)
        // Try clean product URL first, then original short link if different.
        Optional<ProductCommissionInfo> productInfoOpt = productCommissionService.getProductCommission(productUrl);
        if (productInfoOpt.isEmpty() && !productUrl.equals(originalUrl.trim())) {
            log.info("EstimateCashbackUseCase: Retry commission with original URL: {}", originalUrl);
            productInfoOpt = productCommissionService.getProductCommission(originalUrl.trim());
        }

        if (productInfoOpt.isEmpty()) {
            log.warn("EstimateCashbackUseCase: Failed to get commission for URL: {}", productUrl);
            throw new BusinessException("Unable to fetch product commission. Please try again later.");
        }

        ProductCommissionInfo productInfo = productInfoOpt.get();

        // Step 3: Calculate commissions with proper caps
        BigDecimal price = productInfo.price();
        BigDecimal sellerRate = productInfo.sellerCommissionRate();
        BigDecimal shopeeRate = productInfo.shopeeCommissionRate();

        // Calculate seller commission (no cap)
        BigDecimal sellerCommission = BigDecimal.ZERO;
        if (sellerRate != null && price != null) {
            sellerCommission = price.multiply(sellerRate).setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate shopee commission (capped at 50k)
        BigDecimal shopeeCommission = BigDecimal.ZERO;
        if (shopeeRate != null && price != null) {
            shopeeCommission = price.multiply(shopeeRate).setScale(2, RoundingMode.HALF_UP);
            // Apply cap: max 50,000 VND
            if (shopeeCommission.compareTo(SHOPEE_COMMISSION_CAP) > 0) {
                shopeeCommission = SHOPEE_COMMISSION_CAP;
            }
        }

        // Total estimated cashback from rate split
        BigDecimal estimatedCashback = sellerCommission.add(shopeeCommission);

        // Fallback when provider only returns total commission (e.g. ChietKhau, some STK responses)
        if (estimatedCashback.compareTo(BigDecimal.ZERO) == 0
            && productInfo.commission() != null
            && productInfo.commission().compareTo(BigDecimal.ZERO) > 0) {
            estimatedCashback = productInfo.commission();
            log.info("EstimateCashbackUseCase: Using provider commission amount as cashback: {}",
                estimatedCashback);
        }

        // Convert rates to percentage for display (0.05 -> 5.0%)
        BigDecimal sellerRatePercent = toPercent(sellerRate);
        BigDecimal shopeeRatePercent = toPercent(shopeeRate);
        BigDecimal cashbackRatePercent = toPercent(productInfo.commissionRate());

        log.info("EstimateCashbackUseCase: Product: {}, Price: {}, SellerRate: {}%, ShopeeRate: {}%, " +
                "SellerCommission: {}, ShopeeCommission: {} (cap 50k), TotalCashback: {}",
            productInfo.productName(), price, sellerRatePercent, shopeeRatePercent,
            sellerCommission, shopeeCommission, estimatedCashback);

        // Step 4: Build response
        String formattedCashback = VND_FORMAT.format(estimatedCashback) + "đ";
        String message = String.format(
            "Bạn sẽ nhận được ~%s khi mua sản phẩm này qua CashBee!",
            formattedCashback
        );

        return EstimateCashbackResponse.builder()
            .productName(productInfo.productName())
            .shopName(productInfo.shopName())
            .price(price)
            .imageUrl(productInfo.imageUrl())
            .productLink(productInfo.productLink())
            .sales(productInfo.sales())
            .sellerCommissionRate(sellerRatePercent)
            .shopeeCommissionRate(shopeeRatePercent)
            .sellerCommission(sellerCommission)        // Tiền từ seller (không giới hạn)
            .shopeeCommission(shopeeCommission)        // Tiền từ Shopee (đã cap 50k)
            .chietKhauCommission(productInfo.commission())  // Giá trị gốc từ API
            .estimatedCashback(estimatedCashback)      // Tổng đã tính đúng
            .cashbackRate(cashbackRatePercent)
            .isCapped(shopeeCommission.compareTo(SHOPEE_COMMISSION_CAP) == 0)  // True nếu đạt cap
            .maxCap(SHOPEE_COMMISSION_CAP)
            .message(message)
            .build();
    }

    /**
     * Convert rate to percentage (0.15 -> 15.0%).
     * Returns null if input is null.
     *
     * @param rate Rate as decimal (e.g., 0.15 for 15%)
     * @return Rate as percentage or null
     */
    private BigDecimal toPercent(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        return rate.multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean looksLikeShopeeUrl(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("shopee.vn")
            || lower.contains("s.shopee.")
            || lower.contains("shp.ee")
            || lower.contains("shope.ee")
            || lower.contains("shopeefood");
    }
}
