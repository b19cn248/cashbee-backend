package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.user.UpdateUserLevelCommand;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.port.UserDtoMapper;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Update user level (admin operation).
 *
 * This use case allows admin to change a user's tier level.
 * Useful for:
 * - Promoting special customers to DIAMOND (100% cashback)
 * - Adjusting user levels manually
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateUserLevelUseCase {

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;

    /**
     * Execute use case to update user level.
     *
     * @param userId User ID to update
     * @param command Command containing new user level
     * @return Updated user response
     */
    @Transactional
    public UserResponse execute(Long userId, UpdateUserLevelCommand command) {
        log.info("Updating user level: userId={}, newLevel={}", userId, command.getUserLevel());

        // 1. Find user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        log.debug("Found user: id={}, username={}, currentLevel={}",
                user.getId(), user.getUsername(), user.getUserLevel());

        // 2. Update user level using domain method
        user.upgradeTo(command.getUserLevel());

        // 3. Save updated user
        user = userRepository.save(user);

        log.info("User level updated successfully: userId={}, newLevel={}",
                user.getId(), user.getUserLevel());

        // 4. Return response
        return userDtoMapper.toResponse(user);
    }
}
