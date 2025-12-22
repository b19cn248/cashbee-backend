package com.cashbee.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho API kiểm tra số điện thoại tồn tại.
 *
 * Trả về:
 * - phone: Số điện thoại đã kiểm tra
 * - exists: true nếu số điện thoại đã tồn tại trong hệ thống, false nếu chưa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPhoneExistsResponse {

    /**
     * Số điện thoại đã được kiểm tra
     */
    private String phone;

    /**
     * Kết quả kiểm tra: true = đã tồn tại, false = chưa tồn tại
     */
    private boolean exists;
}
