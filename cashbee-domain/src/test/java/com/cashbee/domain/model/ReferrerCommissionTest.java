package com.cashbee.domain.model;

import com.cashbee.domain.enums.ReferrerCommissionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReferrerCommission domain model.
 *
 * @author CashBee Team
 */
@DisplayName("ReferrerCommission Domain Model Tests")
class ReferrerCommissionTest {

    @Nested
    @DisplayName("Commission Calculation")
    class CommissionCalculationTests {

        @Test
        @DisplayName("Should calculate 5% commission correctly")
        void shouldCalculateFivePercentCommission() {
            // Given
            BigDecimal originalCommission = new BigDecimal("10000");

            // When
            BigDecimal commission = ReferrerCommission.calculateCommission(originalCommission);

            // Then
            assertEquals(new BigDecimal("500"), commission);
        }

        @Test
        @DisplayName("Should round down commission to nearest integer")
        void shouldRoundDownCommission() {
            // Given
            BigDecimal originalCommission = new BigDecimal("10001"); // 5% = 500.05

            // When
            BigDecimal commission = ReferrerCommission.calculateCommission(originalCommission);

            // Then
            assertEquals(new BigDecimal("500"), commission);
        }

        @Test
        @DisplayName("Should return zero for null commission")
        void shouldReturnZeroForNullCommission() {
            // When
            BigDecimal commission = ReferrerCommission.calculateCommission(null);

            // Then
            assertEquals(BigDecimal.ZERO, commission);
        }

        @Test
        @DisplayName("Should return zero for zero commission")
        void shouldReturnZeroForZeroCommission() {
            // When
            BigDecimal commission = ReferrerCommission.calculateCommission(BigDecimal.ZERO);

            // Then
            assertEquals(BigDecimal.ZERO, commission);
        }

        @Test
        @DisplayName("Should return zero for negative commission")
        void shouldReturnZeroForNegativeCommission() {
            // When
            BigDecimal commission = ReferrerCommission.calculateCommission(new BigDecimal("-1000"));

            // Then
            assertEquals(BigDecimal.ZERO, commission);
        }

        @Test
        @DisplayName("Should calculate large commission correctly")
        void shouldCalculateLargeCommission() {
            // Given - 1 million VND commission
            BigDecimal originalCommission = new BigDecimal("1000000");

            // When
            BigDecimal commission = ReferrerCommission.calculateCommission(originalCommission);

            // Then - 5% = 50,000 VND
            assertEquals(new BigDecimal("50000"), commission);
        }
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create commission with correct values")
        void shouldCreateCommissionWithCorrectValues() {
            // Given
            Long referrerId = 1L;
            Long refereeId = 2L;
            Long sourceOrderId = 100L;
            BigDecimal originalCommission = new BigDecimal("10000");
            LocalDateTime expiresAt = LocalDateTime.now().plusMonths(3);

            // When
            ReferrerCommission commission = ReferrerCommission.create(
                    referrerId, refereeId, sourceOrderId, originalCommission, expiresAt
            );

            // Then
            assertNotNull(commission);
            assertEquals(referrerId, commission.getReferrerId());
            assertEquals(refereeId, commission.getRefereeId());
            assertEquals(sourceOrderId, commission.getSourceOrderId());
            assertEquals(originalCommission, commission.getOriginalCommission());
            assertEquals(new BigDecimal("5.00"), commission.getCommissionRate());
            assertEquals(new BigDecimal("500"), commission.getCommissionAmount());
            assertEquals(ReferrerCommissionStatus.CONFIRMED, commission.getStatus());
            assertNotNull(commission.getConfirmedAt());
            assertEquals(expiresAt, commission.getExpiresAt());
            assertNotNull(commission.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Expiration")
    class ExpirationTests {

        @Test
        @DisplayName("Should not be expired when expiration is in future")
        void shouldNotBeExpiredWhenExpirationInFuture() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            // Then
            assertFalse(commission.isExpired());
            assertTrue(commission.isWithinValidPeriod());
        }

        @Test
        @DisplayName("Should be expired when expiration is in past")
        void shouldBeExpiredWhenExpirationInPast() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();

            // Then
            assertTrue(commission.isExpired());
            assertFalse(commission.isWithinValidPeriod());
        }

        @Test
        @DisplayName("Should not be expired when expiresAt is null")
        void shouldNotBeExpiredWhenExpiresAtNull() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .expiresAt(null)
                    .build();

            // Then
            assertFalse(commission.isExpired());
        }
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should confirm pending commission")
        void shouldConfirmPendingCommission() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .status(ReferrerCommissionStatus.PENDING)
                    .build();

            // When
            commission.confirm();

            // Then
            assertEquals(ReferrerCommissionStatus.CONFIRMED, commission.getStatus());
            assertNotNull(commission.getConfirmedAt());
        }

        @Test
        @DisplayName("Should throw when confirming non-pending commission")
        void shouldThrowWhenConfirmingNonPendingCommission() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .status(ReferrerCommissionStatus.CONFIRMED)
                    .build();

            // When/Then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    commission::confirm
            );
            assertTrue(ex.getMessage().contains("Cannot confirm commission"));
        }

        @Test
        @DisplayName("Should mark confirmed commission as paid")
        void shouldMarkConfirmedCommissionAsPaid() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .status(ReferrerCommissionStatus.CONFIRMED)
                    .build();

            // When
            commission.markAsPaid();

            // Then
            assertEquals(ReferrerCommissionStatus.PAID, commission.getStatus());
            assertNotNull(commission.getPaidAt());
        }

        @Test
        @DisplayName("Should throw when marking non-confirmed commission as paid")
        void shouldThrowWhenMarkingNonConfirmedAsPaid() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .status(ReferrerCommissionStatus.PENDING)
                    .build();

            // When/Then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    commission::markAsPaid
            );
            assertTrue(ex.getMessage().contains("Cannot mark as paid"));
        }
    }

    @Nested
    @DisplayName("Status Checks")
    class StatusCheckTests {

        @Test
        @DisplayName("Should check pending status")
        void shouldCheckPendingStatus() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .status(ReferrerCommissionStatus.PENDING)
                    .build();

            // Then
            assertTrue(commission.isPending());
            assertFalse(commission.isConfirmed());
            assertFalse(commission.isPaid());
        }

        @Test
        @DisplayName("Should check confirmed status")
        void shouldCheckConfirmedStatus() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .status(ReferrerCommissionStatus.CONFIRMED)
                    .build();

            // Then
            assertFalse(commission.isPending());
            assertTrue(commission.isConfirmed());
            assertFalse(commission.isPaid());
        }

        @Test
        @DisplayName("Should check paid status")
        void shouldCheckPaidStatus() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .status(ReferrerCommissionStatus.PAID)
                    .build();

            // Then
            assertFalse(commission.isPending());
            assertFalse(commission.isConfirmed());
            assertTrue(commission.isPaid());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation for valid commission")
        void shouldPassValidationForValidCommission() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .referrerId(1L)
                    .refereeId(2L)
                    .sourceOrderId(100L)
                    .originalCommission(new BigDecimal("10000"))
                    .commissionAmount(new BigDecimal("500"))
                    .status(ReferrerCommissionStatus.CONFIRMED)
                    .expiresAt(LocalDateTime.now().plusMonths(3))
                    .build();

            // When/Then - should not throw
            assertDoesNotThrow(commission::validate);
        }

        @Test
        @DisplayName("Should fail validation when referrerId is null")
        void shouldFailValidationWhenReferrerIdNull() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .refereeId(2L)
                    .sourceOrderId(100L)
                    .originalCommission(new BigDecimal("10000"))
                    .commissionAmount(new BigDecimal("500"))
                    .status(ReferrerCommissionStatus.CONFIRMED)
                    .expiresAt(LocalDateTime.now().plusMonths(3))
                    .build();

            // When/Then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    commission::validate
            );
            assertEquals("Referrer ID is required", ex.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when expiresAt is null")
        void shouldFailValidationWhenExpiresAtNull() {
            // Given
            ReferrerCommission commission = ReferrerCommission.builder()
                    .referrerId(1L)
                    .refereeId(2L)
                    .sourceOrderId(100L)
                    .originalCommission(new BigDecimal("10000"))
                    .commissionAmount(new BigDecimal("500"))
                    .status(ReferrerCommissionStatus.CONFIRMED)
                    .build();

            // When/Then
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    commission::validate
            );
            assertEquals("Expiration date is required", ex.getMessage());
        }
    }
}
