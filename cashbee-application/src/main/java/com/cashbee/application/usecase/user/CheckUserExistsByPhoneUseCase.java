package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.CheckPhoneExistsResponse;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Kiểm tra số điện thoại đã tồn tại trong hệ thống hay chưa.
 *
 * Use case này được sử dụng để:
 * - Kiểm tra số điện thoại trước khi đăng ký
 * - Validate số điện thoại trong các form
 * - Tránh tạo user trùng số điện thoại
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckUserExistsByPhoneUseCase {

    private final UserRepository userRepository;

    /**
     * Kiểm tra số điện thoại đã tồn tại trong hệ thống.
     *
     * @param phone Số điện thoại cần kiểm tra
     * @return Response chứa phone và kết quả kiểm tra (exists = true/false)
     */
    @Transactional(readOnly = true)
    public CheckPhoneExistsResponse execute(String phone) {
        log.debug("Checking if user exists by phone: {}", phone);

        // Gọi repository method
        boolean exists = userRepository.existsByPhone(phone);

        log.debug("Phone {} exists: {}", phone, exists);

        // Build và trả về response
        return CheckPhoneExistsResponse.builder()
                .phone(phone)
                .exists(exists)
                .build();
    }
}
