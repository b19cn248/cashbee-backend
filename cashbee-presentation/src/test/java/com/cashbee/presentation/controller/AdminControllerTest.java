package com.cashbee.presentation.controller;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.user.GetUsersQuery;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.application.usecase.admin.GetSystemStatisticsUseCase;
import com.cashbee.application.usecase.user.GetUsersUseCase;
import com.cashbee.domain.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for AdminController - Users List endpoint.
 *
 * Business Scenarios:
 * 1. GET /api/admin/users - Get all users with pagination
 * 2. GET /api/admin/users?status=ACTIVE - Filter by status
 * 3. GET /api/admin/users?search=email - Search by email/username
 * 4. Validate pagination parameters
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Users List Tests (TDD)")
class AdminControllerTest {

    @Mock
    private GetSystemStatisticsUseCase getSystemStatisticsUseCase;

    @Mock
    private GetUsersUseCase getUsersUseCase;

    @InjectMocks
    private AdminController adminController;

    // Test data
    private UserResponse user1;
    private UserResponse user2;
    private UserResponse bannedUser;

    @BeforeEach
    void setUp() {
        user1 = UserResponse.builder()
                .id(1L)
                .keycloakId("kc-uuid-1")
                .username("user1")
                .email("user1@example.com")
                .fullName("User One")
                .status("ACTIVE")
                .referralCode("CB1234AB")
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        user2 = UserResponse.builder()
                .id(2L)
                .keycloakId("kc-uuid-2")
                .username("user2")
                .email("user2@example.com")
                .fullName("User Two")
                .status("ACTIVE")
                .referralCode("CB5678CD")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        bannedUser = UserResponse.builder()
                .id(3L)
                .keycloakId("kc-uuid-3")
                .username("banned_user")
                .email("banned@example.com")
                .fullName("Banned User")
                .status("BANNED")
                .referralCode("CBBAN123")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ============================================================
    // TEST GROUP 1: Get All Users
    // ============================================================

    @Nested
    @DisplayName("GET /api/admin/users")
    class GetAllUsers {

        @Test
        @DisplayName("Should return 200 OK with paginated users")
        void getUsers_ReturnsOkWithPaginatedUsers() {
            // Given
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                    Arrays.asList(user1, user2, bannedUser),
                    0, 20, 3
            );

            when(getUsersUseCase.execute(any(GetUsersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            ResponseEntity<?> response = adminController.getUsers(null, null, null, null, 0, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            // Verify use case was called
            verify(getUsersUseCase).execute(any(GetUsersQuery.class));
        }

        @Test
        @DisplayName("Should pass default pagination when not provided")
        void getUsers_PassesDefaultPagination() {
            // Given
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                    Collections.emptyList(),
                    0, 20, 0
            );

            when(getUsersUseCase.execute(any(GetUsersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            adminController.getUsers(null, null, null, null, 0, 20);

            // Then
            ArgumentCaptor<GetUsersQuery> queryCaptor = ArgumentCaptor.forClass(GetUsersQuery.class);
            verify(getUsersUseCase).execute(queryCaptor.capture());

            GetUsersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getPage()).isEqualTo(0);
            assertThat(capturedQuery.getSize()).isEqualTo(20);
        }
    }

    // ============================================================
    // TEST GROUP 2: Filter by Status
    // ============================================================

    @Nested
    @DisplayName("Filter by Status")
    class FilterByStatus {

        @Test
        @DisplayName("Should pass status filter to use case")
        void getUsers_PassesStatusFilter() {
            // Given
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                    Arrays.asList(user1, user2),
                    0, 20, 2
            );

            when(getUsersUseCase.execute(any(GetUsersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            adminController.getUsers(UserStatus.ACTIVE, null, null, null, 0, 20);

            // Then
            ArgumentCaptor<GetUsersQuery> queryCaptor = ArgumentCaptor.forClass(GetUsersQuery.class);
            verify(getUsersUseCase).execute(queryCaptor.capture());

            GetUsersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should filter BANNED users correctly")
        void getUsers_FiltersBannedUsers() {
            // Given
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                    Collections.singletonList(bannedUser),
                    0, 20, 1
            );

            when(getUsersUseCase.execute(any(GetUsersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            ResponseEntity<?> response = adminController.getUsers(UserStatus.BANNED, null, null, null, 0, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ArgumentCaptor<GetUsersQuery> queryCaptor = ArgumentCaptor.forClass(GetUsersQuery.class);
            verify(getUsersUseCase).execute(queryCaptor.capture());

            GetUsersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getStatus()).isEqualTo(UserStatus.BANNED);
        }
    }

    // ============================================================
    // TEST GROUP 3: Search Users
    // ============================================================

    @Nested
    @DisplayName("Search Users")
    class SearchUsers {

        @Test
        @DisplayName("Should pass search keyword to use case")
        void getUsers_PassesSearchKeyword() {
            // Given
            String searchKeyword = "user1@";
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                    Collections.singletonList(user1),
                    0, 20, 1
            );

            when(getUsersUseCase.execute(any(GetUsersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            adminController.getUsers(null, searchKeyword, null, null, 0, 20);

            // Then
            ArgumentCaptor<GetUsersQuery> queryCaptor = ArgumentCaptor.forClass(GetUsersQuery.class);
            verify(getUsersUseCase).execute(queryCaptor.capture());

            GetUsersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getSearch()).isEqualTo(searchKeyword);
        }

        @Test
        @DisplayName("Should handle empty search results")
        void getUsers_HandlesEmptySearchResults() {
            // Given
            PageResponse<UserResponse> emptyResponse = PageResponse.of(
                    Collections.emptyList(),
                    0, 20, 0
            );

            when(getUsersUseCase.execute(any(GetUsersQuery.class)))
                    .thenReturn(emptyResponse);

            // When
            ResponseEntity<?> response = adminController.getUsers(null, "nonexistent", null, null, 0, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ============================================================
    // TEST GROUP 4: Pagination Parameters
    // ============================================================

    @Nested
    @DisplayName("Pagination Parameters")
    class PaginationParameters {

        @Test
        @DisplayName("Should pass custom page and size to use case")
        void getUsers_PassesCustomPagination() {
            // Given
            int customPage = 2;
            int customSize = 50;

            PageResponse<UserResponse> pageResponse = PageResponse.of(
                    Collections.emptyList(),
                    customPage, customSize, 100
            );

            when(getUsersUseCase.execute(any(GetUsersQuery.class)))
                    .thenReturn(pageResponse);

            // When
            adminController.getUsers(null, null, null, null, customPage, customSize);

            // Then
            ArgumentCaptor<GetUsersQuery> queryCaptor = ArgumentCaptor.forClass(GetUsersQuery.class);
            verify(getUsersUseCase).execute(queryCaptor.capture());

            GetUsersQuery capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery.getPage()).isEqualTo(2);
            assertThat(capturedQuery.getSize()).isEqualTo(50);
        }
    }

    // ============================================================
    // TEST GROUP 5: Error Handling
    // ============================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should propagate exception when use case throws")
        void getUsers_PropagatesException_WhenUseCaseThrows() {
            // Given
            when(getUsersUseCase.execute(any(GetUsersQuery.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> adminController.getUsers(null, null, null, null, 0, 20))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");
        }
    }
}
