package com.cashbee.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho API kiểm tra mã giới thiệu có tồn tại trong hệ thống hay không.
 *
 * Trả về:
 * - referralCode: Mã giới thiệu đã kiểm tra
 * - exists: true nếu mã giới thiệu này thuộc về một user trong hệ thống, false nếu không
 *
 * Lưu ý phân biệt:
 * - referralCode: Mã giới thiệu CỦA user (user tạo để chia sẻ cho người khác)
 * - referredBy: Mã giới thiệu user ĐÃ DÙNG khi đăng ký (mã của người giới thiệu họ)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckReferralCodeExistsResponse {

    /**
     * Mã giới thiệu đã được kiểm tra
     */
    private String referralCode;

    /**
     * Kết quả kiểm tra: true = mã tồn tại trong hệ thống, false = không tồn tại
     */
    private boolean exists;
}
