package com.cashbee.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho API kiểm tra email tồn tại.
 *
 * Trả về:
 * - email: Email đã kiểm tra
 * - exists: true nếu email đã tồn tại trong hệ thống, false nếu chưa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckEmailExistsResponse {

    /**
     * Email đã được kiểm tra
     */
    private String email;

    /**
     * Kết quả kiểm tra: true = đã tồn tại, false = chưa tồn tại
     */
    private boolean exists;
}
