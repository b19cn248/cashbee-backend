package com.cashbee.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bank JPA Entity for database persistence.
 * This is the infrastructure model with JPA annotations.
 *
 * Maps to 'bank' table in database.
 * Separate from domain model (Bank) to follow Hexagonal Architecture.
 *
 * @author CashBee Team
 */
@Entity
@Table(name = "bank", indexes = {
    @Index(name = "idx_bank_bank_code", columnList = "bank_code", unique = true),
    @Index(name = "idx_bank_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "bank_code", nullable = false, unique = true, length = 50)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 200)
    private String bankName;

    @Column(name = "short_name", nullable = false, length = 100)
    private String shortName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
