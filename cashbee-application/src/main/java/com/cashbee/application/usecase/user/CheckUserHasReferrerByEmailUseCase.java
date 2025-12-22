package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.CheckUserHasReferrerResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use Case: Kiểm tra user có referrer hay chưa dựa trên email.
 *
 * Use case này được sử dụng để:
 * - Kiểm tra user đã nhập mã giới thiệu (referredBy) hay chưa
 * - Frontend có thể ẩn/hiện form nhập mã giới thiệu dựa trên kết quả
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckUserHasReferrerByEmailUseCase {

    /**
     * Ngày cutoff: chỉ user tạo SAU thời điểm này mới được nhập mã giới thiệu.
     * User tạo trước hoặc bằng thời điểm này sẽ bị coi như đã có referrer.
     */
    private static final LocalDateTime REFERRAL_CUTOFF_DATE =
            LocalDateTime.of(2025, 12, 22, 21, 31, 31);

    private final UserRepository userRepository;

    /**
     * Kiểm tra user có referrer hay chưa dựa trên email.
     *
     * @param email Email của user cần kiểm tra
     * @return Response chứa email và hasReferrer (true/false)
     * @throws NotFoundException nếu không tìm thấy user với email này
     */
    @Transactional(readOnly = true)
    public CheckUserHasReferrerResponse execute(String email) {
        log.debug("Checking if user has referrer by email: {}", email);

        // 1. Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> NotFoundException.ofField("User", "email", email));

        // 2. Kiểm tra điều kiện:
        //    - User cũ (tạo trước/bằng cutoff) → hasReferrer = true (không cho nhập mã)
        //    - User mới (tạo sau cutoff) → kiểm tra referredBy có giá trị chưa
        boolean isOldUser = user.getCreatedAt() == null
                || !user.getCreatedAt().isAfter(REFERRAL_CUTOFF_DATE);

        boolean hasReferrer = isOldUser || user.hasReferrer();

        log.debug("User with email {}: createdAt={}, isOldUser={}, hasReferrer={}",
                email, user.getCreatedAt(), isOldUser, hasReferrer);

        // 3. Build và trả về response
        return CheckUserHasReferrerResponse.builder()
                .email(email)
                .hasReferrer(hasReferrer)
                .build();
    }
}
