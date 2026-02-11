package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.CheckReferredByExistsResponse;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Kiểm tra mã giới thiệu đã được người khác sử dụng chưa.
 *
 * Use case này được sử dụng để:
 * - Kiểm tra xem mã giới thiệu đã có ai dùng khi đăng ký chưa
 * - Thống kê hiệu quả của referral program
 *
 * Lưu ý phân biệt:
 * - referralCode: Mã giới thiệu CỦA user (user tạo để chia sẻ cho người khác)
 * - referredBy: Mã giới thiệu user ĐÃ DÙNG khi đăng ký (mã của người giới thiệu họ)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckUserExistsByReferredByUseCase {

    private final UserRepository userRepository;

    /**
     * Kiểm tra mã giới thiệu đã được người khác sử dụng chưa.
     *
     * @param referredBy Mã giới thiệu cần kiểm tra
     * @return Response chứa referredBy và kết quả kiểm tra (exists = true/false)
     */
    @Transactional(readOnly = true)
    public CheckReferredByExistsResponse execute(String referredBy) {
        log.debug("Checking if referredBy code is used: {}", referredBy);

        // Gọi repository method
        boolean exists = userRepository.existsByReferredBy(referredBy);

        log.debug("ReferredBy {} exists: {}", referredBy, exists);

        // Build và trả về response
        return CheckReferredByExistsResponse.builder()
                .referredBy(referredBy)
                .exists(exists)
                .build();
    }
}
