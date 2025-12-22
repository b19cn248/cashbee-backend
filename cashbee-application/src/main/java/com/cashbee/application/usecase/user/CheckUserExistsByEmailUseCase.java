package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.CheckEmailExistsResponse;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Kiểm tra email đã tồn tại trong hệ thống hay chưa.
 *
 * Use case này được sử dụng để:
 * - Kiểm tra email trước khi đăng ký
 * - Validate email trong các form
 * - Tránh tạo user trùng email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckUserExistsByEmailUseCase {

    private final UserRepository userRepository;

    /**
     * Kiểm tra email đã tồn tại trong hệ thống.
     *
     * @param email Email cần kiểm tra
     * @return Response chứa email và kết quả kiểm tra (exists = true/false)
     */
    @Transactional(readOnly = true)
    public CheckEmailExistsResponse execute(String email) {
        log.debug("Checking if user exists by email: {}", email);

        // Gọi repository method đã có sẵn
        boolean exists = userRepository.existsByEmail(email);

        log.debug("Email {} exists: {}", email, exists);

        // Build và trả về response
        return CheckEmailExistsResponse.builder()
                .email(email)
                .exists(exists)
                .build();
    }
}
