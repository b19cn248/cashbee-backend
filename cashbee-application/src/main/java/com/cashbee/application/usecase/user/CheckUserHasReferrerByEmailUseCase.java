package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.CheckUserHasReferrerResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 2. Kiểm tra user có referrer không (sử dụng method có sẵn trong domain)
        boolean hasReferrer = user.hasReferrer();

        log.debug("User with email {} has referrer: {}", email, hasReferrer);

        // 3. Build và trả về response
        return CheckUserHasReferrerResponse.builder()
                .email(email)
                .hasReferrer(hasReferrer)
                .build();
    }
}
