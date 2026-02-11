package com.cashbee.infrastructure.external.buichung.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for buichung.vn /api/time-slots response.
 *
 * Response format:
 * {
 *   "success": true,
 *   "data": ["16-12 Khung: 00:00", ...]
 * }
 *
 * @author CashBee Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuiChungTimeSlotsResponse {

    /**
     * Success flag.
     */
    private boolean success;

    /**
     * List of time slot names.
     */
    private List<String> data = new ArrayList<>();
}
