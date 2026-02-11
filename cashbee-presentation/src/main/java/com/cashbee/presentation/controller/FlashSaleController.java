package com.cashbee.presentation.controller;

import com.cashbee.application.dto.flashsale.FlashSaleProductResponse;
import com.cashbee.application.usecase.flashsale.GetFlashSaleProductsUseCase;
import com.cashbee.application.usecase.flashsale.GetFlashSaleTimeSlotsUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Flash Sale operations.
 *
 * Provides PUBLIC endpoints (no authentication required) for:
 * - Getting flash sale products with affiliate links
 * - Getting available time slots
 *
 * Data is sourced from buichung.vn and product links are converted
 * to CashBee affiliate links for commission tracking.
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/public/flash-sale")
@RequiredArgsConstructor
@Tag(name = "Flash Sale", description = "Flash Sale products with affiliate links (Public API)")
public class FlashSaleController {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleController.class);

    private final GetFlashSaleProductsUseCase getProductsUseCase;
    private final GetFlashSaleTimeSlotsUseCase getTimeSlotsUseCase;

    /**
     * Get Flash Sale products with affiliate links.
     *
     * This is a PUBLIC endpoint - no authentication required.
     * Product links are automatically converted to affiliate links
     * with CashBee's affiliate ID.
     *
     * @param timeSlot Filter by time slot (optional)
     * @param minDiscount Minimum discount percentage, e.g., 90 for >=90% off (optional)
     * @param maxPrice Maximum sale price in VND, e.g., 10000 for <=10K (optional)
     * @param limit Maximum number of products to return (default 50, max 500)
     * @return List of flash sale products
     */
    @GetMapping("/products")
    @Operation(
        summary = "Get Flash Sale products",
        description = "Get list of Flash Sale products with affiliate links. " +
            "Products are sorted by discount percentage (highest first). " +
            "No authentication required."
    )
    public ResponseEntity<ApiResponse<List<FlashSaleProductResponse>>> getProducts(
            @Parameter(description = "Filter by time slot, e.g., '16-12 Khung: 00:00'")
            @RequestParam(required = false) String timeSlot,

            @Parameter(description = "Minimum discount percentage (0-100), e.g., 90 for >=90% off")
            @RequestParam(required = false, defaultValue = "0") Integer minDiscount,

            @Parameter(description = "Maximum sale price in VND, e.g., 10000 for <=10K")
            @RequestParam(required = false) Long maxPrice,

            @Parameter(description = "Maximum number of products (default 50, max 500)")
            @RequestParam(required = false, defaultValue = "50") Integer limit) {

        log.info("API: GET /api/public/flash-sale/products - timeSlot={}, minDiscount={}, maxPrice={}, limit={}",
            timeSlot, minDiscount, maxPrice, limit);

        List<FlashSaleProductResponse> products = getProductsUseCase.execute(
            timeSlot,
            minDiscount,
            maxPrice,
            limit
        );

        String message = String.format("Found %d flash sale products", products.size());
        if (minDiscount != null && minDiscount > 0) {
            message += String.format(" with >=%d%% discount", minDiscount);
        }
        if (maxPrice != null) {
            message += String.format(", price <=%,d VND", maxPrice);
        }

        return ResponseEntity.ok(ApiResponse.success(products, message));
    }

    /**
     * Get available Flash Sale time slots.
     *
     * Returns list of time slots that have flash sale products available.
     *
     * @return List of time slot names
     */
    @GetMapping("/time-slots")
    @Operation(
        summary = "Get Flash Sale time slots",
        description = "Get list of available Flash Sale time slots. No authentication required."
    )
    public ResponseEntity<ApiResponse<List<String>>> getTimeSlots() {
        log.info("API: GET /api/public/flash-sale/time-slots");

        List<String> timeSlots = getTimeSlotsUseCase.execute();

        return ResponseEntity.ok(
            ApiResponse.success(timeSlots, "Found " + timeSlots.size() + " time slots")
        );
    }
}
