package com.cashbee.application.usecase.flashsale;

import com.cashbee.application.dto.flashsale.FlashSaleProductResponse;
import com.cashbee.domain.flashsale.FlashSaleProductData;
import com.cashbee.domain.flashsale.FlashSaleTimeSlotData;
import com.cashbee.domain.flashsale.FlashSaleDataProvider;
import com.cashbee.application.service.FlashSaleAffiliateLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Use case for retrieving Flash Sale products with affiliate links.
 *
 * This use case:
 * 1. Fetches flash sale data via FlashSaleDataProvider port
 * 2. Filters products by time slot, discount, price
 * 3. Converts product links to affiliate links
 * 4. Returns sorted list of products
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetFlashSaleProductsUseCase {

    private final FlashSaleDataProvider flashSaleDataProvider;
    private final FlashSaleAffiliateLinkService affiliateLinkService;

    /**
     * Default limit for number of products returned.
     */
    private static final int DEFAULT_LIMIT = 50;

    /**
     * Maximum limit for number of products returned.
     */
    private static final int MAX_LIMIT = 500;

    /**
     * Execute use case to get flash sale products.
     *
     * @param timeSlot Filter by time slot (null for all)
     * @param minDiscount Minimum discount percentage (null or 0 for no filter)
     * @param maxPrice Maximum sale price in VND (null for no filter)
     * @param limit Maximum number of products to return (default 50, max 500)
     * @return List of flash sale products with affiliate links
     */
    @Transactional(readOnly = true)
    public List<FlashSaleProductResponse> execute(
            String timeSlot,
            Integer minDiscount,
            Long maxPrice,
            Integer limit) {

        log.info("GetFlashSaleProducts: timeSlot={}, minDiscount={}, maxPrice={}, limit={}",
            timeSlot, minDiscount, maxPrice, limit);

        // Step 1: Fetch data via port interface
        Map<String, FlashSaleTimeSlotData> data = flashSaleDataProvider.getFlashSaleData();

        if (data.isEmpty()) {
            log.warn("GetFlashSaleProducts: No data received from flash sale provider");
            return Collections.emptyList();
        }

        // Step 2: Flatten all products from all time slots
        List<FlashSaleProductData> allProducts = new ArrayList<>();

        data.forEach((slot, slotData) -> {
            // Filter by timeSlot if specified
            if (timeSlot == null || timeSlot.isBlank() || timeSlot.equals(slot)) {
                if (slotData.getProducts() != null) {
                    allProducts.addAll(slotData.getProducts());
                }
            }
        });

        log.info("GetFlashSaleProducts: Found {} products before filtering", allProducts.size());

        // Step 3: Apply filters
        Stream<FlashSaleProductData> stream = allProducts.stream();

        // Filter by minimum discount
        if (minDiscount != null && minDiscount > 0) {
            final int minDiscountFinal = minDiscount;
            stream = stream.filter(p ->
                p.getDiscountPercent() != null && p.getDiscountPercent() >= minDiscountFinal
            );
        }

        // Filter by maximum price
        if (maxPrice != null && maxPrice > 0) {
            final long maxPriceFinal = maxPrice;
            stream = stream.filter(p -> {
                Long price = p.getSalePrice();
                return price != null && price <= maxPriceFinal;
            });
        }

        // Filter out products with no stock
        stream = stream.filter(p -> p.getStockLeft() != null && p.getStockLeft() > 0);

        // Step 4: Sort by discount percentage (highest first)
        List<FlashSaleProductData> filtered = stream
            .sorted((a, b) -> {
                int pctA = a.getDiscountPercent() != null ? a.getDiscountPercent() : 0;
                int pctB = b.getDiscountPercent() != null ? b.getDiscountPercent() : 0;
                return Integer.compare(pctB, pctA); // DESC
            })
            .limit(calculateLimit(limit))
            .collect(Collectors.toList());

        log.info("GetFlashSaleProducts: {} products after filtering", filtered.size());

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 5: Convert links to affiliate links (batch)
        List<String> originalLinks = filtered.stream()
            .map(FlashSaleProductData::getLink)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Map<String, String> affiliateLinks = affiliateLinkService.convertBatch(originalLinks);

        // Step 6: Map to response DTOs
        List<FlashSaleProductResponse> response = filtered.stream()
            .map(product -> mapToResponse(product, affiliateLinks))
            .collect(Collectors.toList());

        log.info("GetFlashSaleProducts: Returning {} products", response.size());
        return response;
    }

    /**
     * Map FlashSaleProductData to FlashSaleProductResponse.
     *
     * @param product Product data from provider
     * @param affiliateLinks Map of original link to affiliate link
     * @return Response DTO
     */
    private FlashSaleProductResponse mapToResponse(
            FlashSaleProductData product,
            Map<String, String> affiliateLinks) {

        String originalLink = product.getLink();
        String affiliateLink = affiliateLinks.getOrDefault(originalLink, originalLink);

        return FlashSaleProductResponse.builder()
            .id(product.getId())
            .imageUrl(product.getImageUrl())
            .title(product.getTitle())
            .originalLink(originalLink)
            .affiliateLink(affiliateLink)
            .salePrice(product.getSalePrice())
            .originalPrice(product.getOriginalPrice())
            .discountPercent(product.getDiscountPercent())
            .stockLeft(product.getStockLeft())
            .timeSlot(product.getTimeSlot())
            .platform("shopee")
            .build();
    }

    /**
     * Calculate limit with bounds checking.
     *
     * @param requestedLimit Requested limit from user
     * @return Actual limit to use
     */
    private int calculateLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }
}
