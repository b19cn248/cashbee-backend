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
 * - commissionRate = sellerCommissionRate + shopeeCommissionRate
 * - estimatedCashback = commissionRate * price
 * - cashbackRate (%) = commissionRate * 100
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
     * Execute the use case to estimate cashback.
     *
     * @param request Request containing Shopee URL
     * @return Response with estimated cashback and product details
     * @throws BusinessException if URL is invalid or API call fails
     */
    public EstimateCashbackResponse execute(EstimateCashbackRequest request) {
        log.info("EstimateCashbackUseCase: Estimating cashback for URL: {}", request.getShopeeUrl());

        // Step 1: Validate and parse URL
        String productUrl = request.getShopeeUrl();
        try {
            ShopeeUrlParser.ParsedShopeeUrl parsedUrl = shopeeUrlParser.parse(productUrl);
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

        // Step 3: Get values directly from adapter (already calculated)
        BigDecimal estimatedCashback = productInfo.commission();
        BigDecimal price = productInfo.price();
        BigDecimal commissionRate = productInfo.commissionRate();

        // Convert rate to percentage (0.15 -> 15.0%)
        BigDecimal cashbackRatePercent = commissionRate
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);

        log.info("EstimateCashbackUseCase: Product: {}, Price: {}, Rate: {}%, Cashback: {}",
            productInfo.productName(), price, cashbackRatePercent, estimatedCashback);

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
            .chietKhauCommission(estimatedCashback)
            .estimatedCashback(estimatedCashback)
            .cashbackRate(cashbackRatePercent)
            .isCapped(productInfo.isCapped())
            .maxCap(productInfo.maxCap())
            .message(message)
            .build();
    }
}
