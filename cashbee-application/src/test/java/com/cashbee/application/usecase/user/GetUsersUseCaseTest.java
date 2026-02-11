package com.cashbee.application.usecase.user;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.user.GetUsersQuery;
import com.cashbee.application.dto.user.UserResponse;
import com.cashbee.domain.enums.UserStatus;
import com.cashbee.domain.model.User;
import com.cashbee.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for GetUsersUseCase.
 *
 * Business Scenarios:
 * 1. Admin gets all users with pagination
 * 2. Admin filters users by status
 * 3. Admin searches users by email/username
 * 4. Empty result when no users found
 * 5. Query validation (pagination limits)
 *
 * @author CashBee Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetUsersUseCase Tests (TDD)")
class GetUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUsersUseCase getUsersUseCase;

    // Test data
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .keycloakId("kc-uuid-1")
                .username("user1")
                .email("user1@example.com")
                .fullName("User One")
                .status(UserStatus.ACTIVE)
                .referralCode("CB1234AB")
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        user2 = User.builder()
                .id(2L)
                .keycloakId("kc-uuid-2")
                .username("user2")
                .email("user2@example.com")
                .fullName("User Two")
                .status(UserStatus.ACTIVE)
                .referralCode("CB5678CD")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        user3 = User.builder()
                .id(3L)
                .keycloakId("kc-uuid-3")
                .username("banned_user")
                .email("banned@example.com")
                .fullName("Banned User")
                .status(UserStatus.BANNED)
                .referralCode("CBBAN123")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ============================================================
    // TEST GROUP 1: Get All Users with Pagination
    // ============================================================

    @Nested
    @DisplayName("Get All Users - Happy Path")
    class GetAllUsersHappyPath {

        @Test
        @DisplayName("Should get all users with default pagination")
        void execute_GetsAllUsersWithPagination_Success() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .page(0)
                    .size(20)
                    .build();

            List<User> users = Arrays.asList(user1, user2, user3);
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 20), 3);

            when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

            // When
            PageResponse<UserResponse> result = getUsersUseCase.execute(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(20);

            // Verify user data mapping
            UserResponse firstUser = result.getContent().get(0);
            assertThat(firstUser.getId()).isEqualTo(1L);
            assertThat(firstUser.getUsername()).isEqualTo("user1");
            assertThat(firstUser.getEmail()).isEqualTo("user1@example.com");
            assertThat(firstUser.getStatus()).isEqualTo("ACTIVE");

            // Verify repository called
            verify(userRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void execute_HandlesPagination_Correctly() {
            // Given - Request page 2 with size 10
            GetUsersQuery query = GetUsersQuery.builder()
                    .page(1)
                    .size(10)
                    .build();

            List<User> users = Collections.singletonList(user1);
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(1, 10), 25);

            when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

            // When
            PageResponse<UserResponse> result = getUsersUseCase.execute(query);

            // Then
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.isFirst()).isFalse();
            assertThat(result.isLast()).isFalse();

            // Verify pageable parameters
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findAll(pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }
    }

    // ============================================================
    // TEST GROUP 2: Filter by Status
    // ============================================================

    @Nested
    @DisplayName("Filter by Status")
    class FilterByStatus {

        @Test
        @DisplayName("Should filter users by ACTIVE status")
        void execute_FiltersUsersByStatus_Success() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .status(UserStatus.ACTIVE)
                    .page(0)
                    .size(20)
                    .build();

            List<User> activeUsers = Arrays.asList(user1, user2);
            Page<User> userPage = new PageImpl<>(activeUsers, PageRequest.of(0, 20), 2);

            when(userRepository.findByStatus(eq(UserStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            PageResponse<UserResponse> result = getUsersUseCase.execute(query);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(u -> u.getStatus().equals("ACTIVE"));

            // Verify correct repository method called
            verify(userRepository).findByStatus(eq(UserStatus.ACTIVE), any(Pageable.class));
            verify(userRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter users by BANNED status")
        void execute_FiltersBannedUsers_Success() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .status(UserStatus.BANNED)
                    .page(0)
                    .size(20)
                    .build();

            List<User> bannedUsers = Collections.singletonList(user3);
            Page<User> userPage = new PageImpl<>(bannedUsers, PageRequest.of(0, 20), 1);

            when(userRepository.findByStatus(eq(UserStatus.BANNED), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            PageResponse<UserResponse> result = getUsersUseCase.execute(query);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("BANNED");
        }
    }

    // ============================================================
    // TEST GROUP 3: Search Users
    // ============================================================

    @Nested
    @DisplayName("Search Users")
    class SearchUsers {

        @Test
        @DisplayName("Should search users by email keyword")
        void execute_SearchesByEmail_Success() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .search("user1@")
                    .page(0)
                    .size(20)
                    .build();

            List<User> matchedUsers = Collections.singletonList(user1);
            Page<User> userPage = new PageImpl<>(matchedUsers, PageRequest.of(0, 20), 1);

            when(userRepository.searchByEmailOrUsername(eq("user1@"), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            PageResponse<UserResponse> result = getUsersUseCase.execute(query);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmail()).contains("user1@");

            // Verify search method called
            verify(userRepository).searchByEmailOrUsername(eq("user1@"), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search users by username keyword")
        void execute_SearchesByUsername_Success() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .search("banned")
                    .page(0)
                    .size(20)
                    .build();

            List<User> matchedUsers = Collections.singletonList(user3);
            Page<User> userPage = new PageImpl<>(matchedUsers, PageRequest.of(0, 20), 1);

            when(userRepository.searchByEmailOrUsername(eq("banned"), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            PageResponse<UserResponse> result = getUsersUseCase.execute(query);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).contains("banned");
        }
    }

    // ============================================================
    // TEST GROUP 4: Empty Results
    // ============================================================

    @Nested
    @DisplayName("Empty Results")
    class EmptyResults {

        @Test
        @DisplayName("Should return empty page when no users found")
        void execute_ReturnsEmptyPage_WhenNoUsersFound() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .search("nonexistent")
                    .page(0)
                    .size(20)
                    .build();

            Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(userRepository.searchByEmailOrUsername(anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            PageResponse<UserResponse> result = getUsersUseCase.execute(query);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getTotalPages()).isEqualTo(0);
        }
    }

    // ============================================================
    // TEST GROUP 5: Query Validation
    // ============================================================

    @Nested
    @DisplayName("Query Validation")
    class QueryValidation {

        @Test
        @DisplayName("Should normalize negative page to 0")
        void execute_NormalizesNegativePage_ToZero() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .page(-5)
                    .size(20)
                    .build();

            Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(userRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // When
            getUsersUseCase.execute(query);

            // Then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findAll(pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should cap page size to maximum 100")
        void execute_CapsPageSize_ToMaximum100() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .page(0)
                    .size(500)  // Too large
                    .build();

            Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 100), 0);
            when(userRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // When
            getUsersUseCase.execute(query);

            // Then
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findAll(pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should trim search keyword")
        void execute_TrimsSearchKeyword() {
            // Given
            GetUsersQuery query = GetUsersQuery.builder()
                    .search("  user  ")  // With spaces
                    .page(0)
                    .size(20)
                    .build();

            Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(userRepository.searchByEmailOrUsername(anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            getUsersUseCase.execute(query);

            // Then
            verify(userRepository).searchByEmailOrUsername(eq("user"), any(Pageable.class));
        }
    }
}
