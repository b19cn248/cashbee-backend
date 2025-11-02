package com.cashbee.application.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Command DTO for adding pending balance to wallet.
 *
 * Used when a new cashback is earned but not yet confirmed.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPendingBalanceCommand {

    /**
     * User ID whose wallet to update.
     */
    @NotNull(message = "User ID is required")
    private Long userId;

    /**
     * Amount to add to pending balance.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Description/reason for adding pending balance.
     */
    private String description;
}
