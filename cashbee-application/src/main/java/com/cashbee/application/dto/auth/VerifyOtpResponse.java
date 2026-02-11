package com.cashbee.application.dto.auth;

import java.time.LocalDateTime;

/**
 * Response DTO for successful OTP verification.
 * Contains user information after registration is completed.
 */
public record VerifyOtpResponse(
    Long userId,
    String keycloakId,
    String username,
    String email,
    String fullName,
    String phone,
    String referralCode,
    String referredBy,
    Long walletId,
    String status,
    LocalDateTime createdAt
) {
    /**
     * Builder for VerifyOtpResponse
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String keycloakId;
        private String username;
        private String email;
        private String fullName;
        private String phone;
        private String referralCode;
        private String referredBy;
        private Long walletId;
        private String status;
        private LocalDateTime createdAt;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder keycloakId(String keycloakId) {
            this.keycloakId = keycloakId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder referralCode(String referralCode) {
            this.referralCode = referralCode;
            return this;
        }

        public Builder referredBy(String referredBy) {
            this.referredBy = referredBy;
            return this;
        }

        public Builder walletId(Long walletId) {
            this.walletId = walletId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public VerifyOtpResponse build() {
            return new VerifyOtpResponse(
                userId, keycloakId, username, email, fullName, phone,
                referralCode, referredBy, walletId, status, createdAt
            );
        }
    }
}
