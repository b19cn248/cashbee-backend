package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User JPA Entity for database persistence.
 * This is the infrastructure model with JPA annotations.
 *
 * Maps to 'user' table in database.
 * Separate from domain model (User) to follow Hexagonal Architecture.
 *
 * IMPORTANT: Authentication data (password, roles) are NOT stored here.
 * They are managed by Keycloak. This entity only stores business data.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "user", indexes = {
    @Index(name = "idx_user_keycloak_id", columnList = "keycloak_id", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_referral_code", columnList = "referral_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Keycloak user UUID - links to Keycloak user.
     * NOT NULL and UNIQUE - every local user must have Keycloak account.
     */
    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "referral_code", unique = true, length = 20)
    private String referralCode;

    @Column(name = "referred_by", length = 20)
    private String referredBy;

    @Column(name = "user_level", nullable = false, length = 20)
    private String userLevel;

    /**
     * Referrer tier for commission rate differentiation.
     * BRONZE (5%), SILVER (7%), GOLD (10%).
     */
    @Column(name = "referrer_tier", nullable = false, length = 20)
    private String referrerTier;

    /**
     * Total number of activated referrals this user has.
     * Used to determine referrer tier level.
     */
    @Column(name = "total_activated_referrals", nullable = false)
    private Integer totalActivatedReferrals;

    @Column(name = "total_completed_orders", nullable = false)
    private Integer totalCompletedOrders;

    @Column(name = "referral_activated_at")
    private LocalDateTime referralActivatedAt;

    @Column(name = "referral_expires_at")
    private LocalDateTime referralExpiresAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "has_ever_logged_in", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean hasEverLoggedIn;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.userLevel == null) {
            this.userLevel = "NORMAL";
        }
        if (this.referrerTier == null) {
            this.referrerTier = "BRONZE";
        }
        if (this.totalActivatedReferrals == null) {
            this.totalActivatedReferrals = 0;
        }
        if (this.totalCompletedOrders == null) {
            this.totalCompletedOrders = 0;
        }
        if (this.hasEverLoggedIn == null) {
            this.hasEverLoggedIn = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
