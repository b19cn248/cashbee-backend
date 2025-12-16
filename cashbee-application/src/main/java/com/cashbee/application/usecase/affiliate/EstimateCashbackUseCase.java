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
 * 3. Calculates CashBee's cashback amount using formula:
 *    cashback = externalCommission * (80/60) = externalCommission * 4/3
 * 4. Returns estimated cashback with product details
 *
 * Why multiply by 4/3 (≈1.3333)?
 * - Tui3Gang API returns 60% of full Shopee commission to users
 * - CashBee wants to give 80% of full commission to users
 * - Formula: (commission / 0.6) * 0.8 = commission * (0.8 / 0.6) = commission * 4/3
 *
 * Example:
 * - Tui3Gang returns: 17,009đ (60% of full commission)
 * - Full commission: 17,009 / 0.6 = 28,348đ
 * - CashBee gives 80%: 28,348 * 0.8 = 22,679đ
 * - Or simply: 17,009 * (4/3) = 22,679đ
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstimateCashbackUseCase {

    /**
     * Port for getting product commission data.
     * Implementation is provided by Infrastructure layer (ChietKhauProductCommissionAdapter).
     */
    private final ProductCommissionService productCommissionService;
    private final ShopeeUrlParser shopeeUrlParser;

    /**
     * Tui3Gang gives 60% of full commission to users.
     */
    private static final BigDecimal TUI3GANG_SHARE_RATE = new BigDecimal("0.6");

    /**
     * CashBee wants to give 80% of full commission to users.
     */
    private static final BigDecimal CASHBEE_SHARE_RATE = new BigDecimal("0.8");

    /**
     * Number formatter for Vietnamese currency.
     */
    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    /**
     * Execute the use case to estimate cashback.
     *
     * @param request Request containing Shopee URL
     * @return Response with estimated cashback and product details
     * @throws BusinessException if URL is invalid or API call fails
     */
    public EstimateCashbackResponse execute(EstimateCashbackRequest request) {
        log.info("EstimateCashbackUseCase: Estimating cashback for URL: {}", request.getShopeeUrl());

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

        // Step 3: Calculate cashback
        BigDecimal externalCommission = productInfo.commission();
        BigDecimal price = productInfo.price();

        // Formula: cashback = externalCommission * (80/60) = externalCommission * CASHBEE_SHARE_RATE / TUI3GANG_SHARE_RATE
        // Example: 17,009 * 0.8 / 0.6 = 22,679đ
        BigDecimal estimatedCashback = externalCommission
            .multiply(CASHBEE_SHARE_RATE)
            .divide(TUI3GANG_SHARE_RATE, 0, RoundingMode.HALF_UP);  // Round to whole number (VND)

        // Calculate cashback rate as percentage
        BigDecimal cashbackRate = BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            cashbackRate = estimatedCashback
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        log.info("EstimateCashbackUseCase: Calculated cashback - Tui3Gang (60%): {}, CashBee (80%): {}, Rate: {}%",
            externalCommission, estimatedCashback, cashbackRate);

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
            .message(message)
            .build();
    }
}
