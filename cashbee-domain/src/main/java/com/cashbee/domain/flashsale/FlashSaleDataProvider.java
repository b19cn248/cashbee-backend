package com.cashbee.domain.flashsale;

import java.util.List;
import java.util.Map;

/**
 * Port interface for fetching Flash Sale data from external sources.
 *
 * This is a PORT in Hexagonal Architecture:
 * - Defined in Domain layer (inward)
 * - Implemented by Infrastructure layer (adapter)
 *
 * The implementation (BuiChungFlashSaleAdapter) is in Infrastructure layer.
 *
 * @author CashBee Team
 */
public interface FlashSaleDataProvider {

    /**
     * Get all flash sale data grouped by time slot.
     *
     * @return Map of time slot name to time slot data
     */
    Map<String, FlashSaleTimeSlotData> getFlashSaleData();

    /**
     * Get all flash sale products as a flat list.
     *
     * @return List of all products from all time slots
     */
    List<FlashSaleProductData> getAllProducts();

    /**
     * Get available time slots.
     *
     * @return List of time slot names
     */
    List<String> getTimeSlots();

    /**
     * Check if the flash sale system is active.
     *
     * @return true if system is active
     */
    boolean isSystemActive();
}
