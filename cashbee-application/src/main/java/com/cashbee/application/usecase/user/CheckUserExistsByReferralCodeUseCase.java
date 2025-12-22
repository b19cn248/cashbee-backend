package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.CheckReferralCodeExistsResponse;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Kiểm tra mã giới thiệu có tồn tại trong hệ thống hay không.
 *
 * Use case này được sử dụng để:
 * - Kiểm tra xem mã giới thiệu có thuộc về user nào trong hệ thống không
 * - Validate mã giới thiệu trước khi user nhập vào form referredBy
 *
 * Lưu ý phân biệt:
 * - referralCode: Mã giới thiệu CỦA user (user tạo để chia sẻ cho người khác)
 * - referredBy: Mã giới thiệu user ĐÃ DÙNG khi đăng ký (mã của người giới thiệu họ)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckUserExistsByReferralCodeUseCase {

    private final UserRepository userRepository;

    /**
     * Kiểm tra mã giới thiệu có tồn tại trong hệ thống hay không.
     *
     * @param referralCode Mã giới thiệu cần kiểm tra
     * @return Response chứa referralCode và kết quả kiểm tra (exists = true/false)
     */
    @Transactional(readOnly = true)
    public CheckReferralCodeExistsResponse execute(String referralCode) {
        log.debug("Checking if referralCode exists: {}", referralCode);

        // Gọi repository method
        boolean exists = userRepository.existsByReferralCode(referralCode);

        log.debug("ReferralCode {} exists: {}", referralCode, exists);

        // Build và trả về response
        return CheckReferralCodeExistsResponse.builder()
                .referralCode(referralCode)
                .exists(exists)
                .build();
    }
}
