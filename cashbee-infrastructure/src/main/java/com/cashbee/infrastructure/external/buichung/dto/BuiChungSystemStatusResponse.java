package com.cashbee.infrastructure.external.buichung.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for buichung.vn /api/system-status response.
 *
 * Response format:
 * {
 *   "success": true,
 *   "isActive": true
 * }
 *
 * @author CashBee Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuiChungSystemStatusResponse {

    /**
     * Success flag.
     */
    private boolean success;

    /**
     * Whether the flash sale system is currently active.
     */
    private boolean isActive;
}
