package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.EstimateCashbackRequest;
import com.cashbee.application.dto.affiliate.EstimateCashbackResponse;
import com.cashbee.application.util.affiliate.ShopeeUrlParser;
import com.cashbee.common.exception.BusinessException;
import com.cashbee.domain.enums.UserLevel;
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
 * 3. Calculates CashBee's cashback amount based on user level
 * 4. Returns estimated cashback with product details
 *
 * Formula: cashback = commission / 60 * userPercentage
 *
 * User Level Rates:
 * - NORMAL: 80% of full commission
 * - VIP: 83% of full commission
 * - SUPER: 85% of full commission
 *
 * Example (commission = 17,009đ from T3 API):
 * - T3 API returns 60% of full commission
 * - Full commission = 17,009 / 0.6 = 28,348đ
 * - NORMAL (80%): 28,348 * 0.8 = 22,679đ → commission / 60 * 80
 * - VIP (83%):    28,348 * 0.83 = 23,529đ → commission / 60 * 83
 * - SUPER (85%):  28,348 * 0.85 = 24,096đ → commission / 60 * 85
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstimateCashbackUseCase {

    /**
     * Port for getting product commission data.
     * Implementation is provided by Infrastructure layer (Tui3GangProductCommissionAdapter).
     */
    private final ProductCommissionService productCommissionService;
    private final ShopeeUrlParser shopeeUrlParser;

    /**
     * T3 API returns commission as 60% of full commission.
     * We use 60 as divisor in formula: commission / 60 * userPercentage
     */
    private static final BigDecimal T3_COMMISSION_BASE = new BigDecimal("60");

    /**
     * Number formatter for Vietnamese currency.
     */
    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    /**
     * Execute the use case to estimate cashback.
     *
     * @param request Request containing Shopee URL
     * @param userLevel User level for cashback rate calculation
     * @return Response with estimated cashback and product details
     * @throws BusinessException if URL is invalid or API call fails
     */
    public EstimateCashbackResponse execute(EstimateCashbackRequest request, UserLevel userLevel) {
        log.info("EstimateCashbackUseCase: Estimating cashback for URL: {} with userLevel: {}",
            request.getShopeeUrl(), userLevel);

        // Step 1: Validate and parse URL to ensure it's a valid Shopee URL
        String productUrl = request.getShopeeUrl();
        try {
            ShopeeUrlParser.ParsedShopeeUrl parsedUrl = shopeeUrlParser.parse(productUrl);
            // Use the URL that should be used for affiliate link (expanded if shortened)
            productUrl = parsedUrl.getUrlForAffiliateLink();
            log.info("EstimateCashbackUseCase: Parsed URL - Shop ID: {}, Item ID: {}",
                parsedUrl.getShopId(), parsedUrl.getItemId());
        } catch (IllegalArgumentException e) {
            log.error("EstimateCashbackUseCase: Invalid Shopee URL: {}", request.getShopeeUrl(), e);
            throw new BusinessException("Invalid Shopee URL: " + e.getMessage());
        }

        // Step 2: Call external API via port
        Optional<ProductCommissionInfo> productInfoOpt = productCommissionService.getProductCommission(productUrl);

        if (productInfoOpt.isEmpty()) {
            log.warn("EstimateCashbackUseCase: Failed to get commission for URL: {}", productUrl);
            throw new BusinessException("Unable to fetch product commission. Please try again later.");
        }

        ProductCommissionInfo productInfo = productInfoOpt.get();

        // Step 3: Calculate cashback based on user level
        BigDecimal externalCommission = productInfo.commission();
        BigDecimal price = productInfo.price();

        // Formula: cashback = commission / 60 * userPercentage
        // Example for NORMAL (80%): 17,009 / 60 * 80 = 22,679đ
        // Example for VIP (83%):    17,009 / 60 * 83 = 23,515đ
        // Example for SUPER (85%):  17,009 / 60 * 85 = 24,074đ
        BigDecimal estimatedCashback = externalCommission
            .multiply(userLevel.getCashbackRateDecimal())
            .divide(T3_COMMISSION_BASE, 0, RoundingMode.HALF_UP);  // Round to whole number (VND)

        // Calculate cashback rate as percentage of price
        BigDecimal cashbackRate = BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            cashbackRate = estimatedCashback
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        log.info("EstimateCashbackUseCase: Calculated cashback - T3 (60%): {}, {} ({}%): {}, Rate: {}%",
            externalCommission, userLevel.name(), userLevel.getCashbackRate(), estimatedCashback, cashbackRate);

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
            .sales(productInfo.sales())  // Số lượt bán
            .chietKhauCommission(externalCommission)
            .estimatedCashback(estimatedCashback)
            .cashbackRate(cashbackRate)
            .isCapped(productInfo.isCapped())
            .maxCap(productInfo.maxCap())
            .userLevel(userLevel.name())
            .appliedCashbackRate(userLevel.getCashbackRate())
            .message(message)
            .build();
    }
}
