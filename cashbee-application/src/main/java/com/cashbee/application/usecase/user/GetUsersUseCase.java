package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.user.GetUsersQuery;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for getting users list with pagination and filtering.
 *
 * This use case is used by Admin to:
 * - View all users in the system
 * - Filter users by status (ACTIVE, SUSPENDED, BANNED)
 * - Search users by email or username
 * - Paginate through large user lists
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUsersUseCase {

    private final UserRepository userRepository;

    /**
     * Execute the use case to get users list.
     *
     * @param query Query parameters including filters and pagination
     * @return Paginated response with users
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> execute(GetUsersQuery query) {
        // Validate and normalize query parameters
        query.validate();

        log.info("Getting users list (page: {}, size: {}, status: {}, search: {})",
                query.getPage(), query.getSize(), query.getStatus(), query.getSearch());

        // Create pageable with sorting by createdAt DESC (newest first)
        Pageable pageable = PageRequest.of(
                query.getPage(),
                query.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Fetch users based on filters
        Page<User> userPage;

        if (query.hasSearchFilter()) {
            // Search by email or username
            userPage = userRepository.searchByEmailOrUsername(query.getSearch(), pageable);
            log.debug("Search found {} users matching '{}'",
                    userPage.getTotalElements(), query.getSearch());

        } else if (query.hasStatusFilter()) {
            // Filter by status
            userPage = userRepository.findByStatus(query.getStatus(), pageable);
            log.debug("Found {} users with status {}",
                    userPage.getTotalElements(), query.getStatus());

        } else {
            // Get all users
            userPage = userRepository.findAll(pageable);
            log.debug("Found {} total users", userPage.getTotalElements());
        }

        // Map to response DTOs
        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("Returning {} users (page {} of {})",
                userResponses.size(), query.getPage(), userPage.getTotalPages());

        // Build paginated response
        return PageResponse.of(
                userResponses,
                query.getPage(),
                query.getSize(),
                userPage.getTotalElements()
        );
    }

    /**
     * Map domain user to response DTO.
     */
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .referralCode(user.getReferralCode())
                .referredBy(user.getReferredBy())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .userLevel(user.getUserLevel() != null ? user.getUserLevel().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
