package com.cashbee.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho API kiểm tra user có referrer hay chưa.
 *
 * Trả về:
 * - email: Email của user đã kiểm tra
 * - hasReferrer: true nếu user đã có referredBy, false nếu chưa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckUserHasReferrerResponse {

    /**
     * Email của user đã được kiểm tra
     */
    private String email;

    /**
     * Kết quả kiểm tra: true = đã có referrer, false = chưa có referrer
     */
    private boolean hasReferrer;
}
