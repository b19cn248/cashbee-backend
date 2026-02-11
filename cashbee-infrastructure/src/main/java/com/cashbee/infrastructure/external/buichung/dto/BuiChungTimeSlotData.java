package com.cashbee.infrastructure.external.buichung.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing time slot data from buichung.vn Flash Sale API.
 *
 * Each time slot contains:
 * - linkMapping: Maps original links to mapped links
 * - subIdMapping: Sub ID mappings (usually empty)
 * - reasonMapping: Reason mappings (usually empty)
 * - productCache: Map of product ID to product details
 *
 * @author CashBee Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuiChungTimeSlotData {

    /**
     * Link mapping: original link -> mapped link.
     * Used for redirecting/tracking purposes.
     */
    private Map<String, String> linkMapping = new HashMap<>();

    /**
     * Sub ID mapping (usually empty).
     */
    private Map<String, String> subIdMapping = new HashMap<>();

    /**
     * Reason mapping (usually empty).
     */
    private Map<String, String> reasonMapping = new HashMap<>();

    /**
     * Product cache: product ID -> product details.
     * Contains all flash sale products for this time slot.
     */
    private Map<String, BuiChungProduct> productCache = new HashMap<>();

    /**
     * Get the number of products in this time slot.
     *
     * @return Number of products
     */
    public int getProductCount() {
        return productCache != null ? productCache.size() : 0;
    }
}
