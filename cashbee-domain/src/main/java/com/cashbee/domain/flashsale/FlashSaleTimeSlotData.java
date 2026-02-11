package com.cashbee.domain.flashsale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Flash Sale time slot data.
 *
 * This is a DOMAIN layer DTO used for data transfer between
 * Infrastructure (adapters) and Application (use cases).
 *
 * @author CashBee Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleTimeSlotData {

    /**
     * Time slot name (e.g., "16-12 Khung: 00:00").
     */
    private String timeSlotName;

    /**
     * Link mapping for the time slot.
     */
    private Map<String, String> linkMapping;

    /**
     * List of products in this time slot.
     */
    private List<FlashSaleProductData> products;

    /**
     * Get number of products in this time slot.
     *
     * @return Product count
     */
    public int getProductCount() {
        return products != null ? products.size() : 0;
    }
}
