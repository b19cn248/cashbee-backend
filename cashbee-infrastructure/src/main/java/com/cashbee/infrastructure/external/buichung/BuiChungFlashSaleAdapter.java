package com.cashbee.infrastructure.external.buichung;

import com.cashbee.domain.flashsale.FlashSaleProductData;
import com.cashbee.domain.flashsale.FlashSaleTimeSlotData;
import com.cashbee.domain.flashsale.FlashSaleDataProvider;
import com.cashbee.infrastructure.external.buichung.dto.BuiChungProduct;
import com.cashbee.infrastructure.external.buichung.dto.BuiChungTimeSlotData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter for BuiChung Flash Sale API.
 *
 * This is an ADAPTER in Hexagonal Architecture:
 * - Implements Port interface defined in Application layer
 * - Converts Infrastructure DTOs to Application DTOs
 * - Delegates to BuiChungFlashSaleService for actual API calls
 *
 * @author CashBee Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BuiChungFlashSaleAdapter implements FlashSaleDataProvider {

    private final BuiChungFlashSaleService buiChungService;

    /**
     * Get all flash sale data grouped by time slot.
     *
     * @return Map of time slot name to time slot data
     */
    @Override
    public Map<String, FlashSaleTimeSlotData> getFlashSaleData() {
        log.debug("BuiChungFlashSaleAdapter: Fetching flash sale data");

        Map<String, BuiChungTimeSlotData> rawData = buiChungService.getFlashSaleData();

        if (rawData.isEmpty()) {
            return Collections.emptyMap();
        }

        // Convert to Application layer DTOs
        Map<String, FlashSaleTimeSlotData> result = new HashMap<>();

        rawData.forEach((timeSlotName, slotData) -> {
            List<FlashSaleProductData> products = convertProducts(slotData, timeSlotName);

            FlashSaleTimeSlotData convertedSlot = FlashSaleTimeSlotData.builder()
                .timeSlotName(timeSlotName)
                .linkMapping(slotData.getLinkMapping())
                .products(products)
                .build();

            result.put(timeSlotName, convertedSlot);
        });

        log.debug("BuiChungFlashSaleAdapter: Converted {} time slots", result.size());
        return result;
    }

    /**
     * Get all flash sale products as a flat list.
     *
     * @return List of all products from all time slots
     */
    @Override
    public List<FlashSaleProductData> getAllProducts() {
        log.debug("BuiChungFlashSaleAdapter: Fetching all products");

        Map<String, BuiChungTimeSlotData> rawData = buiChungService.getFlashSaleData();

        if (rawData.isEmpty()) {
            return Collections.emptyList();
        }

        List<FlashSaleProductData> allProducts = new ArrayList<>();

        rawData.forEach((timeSlotName, slotData) -> {
            List<FlashSaleProductData> products = convertProducts(slotData, timeSlotName);
            allProducts.addAll(products);
        });

        log.debug("BuiChungFlashSaleAdapter: Retrieved {} total products", allProducts.size());
        return allProducts;
    }

    /**
     * Get available time slots.
     *
     * @return List of time slot names
     */
    @Override
    public List<String> getTimeSlots() {
        log.debug("BuiChungFlashSaleAdapter: Fetching time slots");
        return buiChungService.getTimeSlots();
    }

    /**
     * Check if the flash sale system is active.
     *
     * @return true if system is active
     */
    @Override
    public boolean isSystemActive() {
        return buiChungService.isSystemActive();
    }

    /**
     * Convert BuiChung products to Application layer DTOs.
     *
     * @param slotData Time slot data containing products
     * @param timeSlotName Name of the time slot
     * @return List of converted products
     */
    private List<FlashSaleProductData> convertProducts(BuiChungTimeSlotData slotData, String timeSlotName) {
        if (slotData.getProductCache() == null) {
            return Collections.emptyList();
        }

        return slotData.getProductCache().values().stream()
            .map(product -> convertProduct(product, timeSlotName))
            .collect(Collectors.toList());
    }

    /**
     * Convert single BuiChungProduct to FlashSaleProductData.
     *
     * @param product BuiChung product
     * @param timeSlotName Name of the time slot
     * @return Converted product data
     */
    private FlashSaleProductData convertProduct(BuiChungProduct product, String timeSlotName) {
        return FlashSaleProductData.builder()
            .id(product.getId())
            .imageUrl(product.getImg())
            .title(product.getTitle())
            .link(product.getLink())
            .salePrice(product.getPriceAsLong())
            .originalPrice(product.getOriginalPrice())
            .discountPercent(product.getPercent())
            .stockLeft(product.getAmount())
            .timeSlot(timeSlotName)
            .build();
    }
}
