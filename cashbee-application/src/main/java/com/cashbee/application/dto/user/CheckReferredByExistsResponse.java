package com.cashbee.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho API kiểm tra mã giới thiệu đã được sử dụng chưa.
 *
 * Trả về:
 * - referredBy: Mã giới thiệu đã kiểm tra
 * - exists: true nếu mã giới thiệu này đã được người khác sử dụng, false nếu chưa
 *
 * Lưu ý:
 * - referralCode: Mã giới thiệu CỦA user (user tạo để chia sẻ)
 * - referredBy: Mã giới thiệu user ĐÃ DÙNG khi đăng ký (mã của người giới thiệu họ)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckReferredByExistsResponse {

    /**
     * Mã giới thiệu đã được kiểm tra
     */
    private String referredBy;

    /**
     * Kết quả kiểm tra: true = đã có người sử dụng mã này, false = chưa ai sử dụng
     */
    private boolean exists;
}
