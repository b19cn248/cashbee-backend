package com.cashbee.application.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Command DTO for confirming pending balance.
 *
 * Used when pending cashback is confirmed and becomes available balance.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPendingBalanceCommand {

    /**
     * User ID whose wallet to update.
     */
    @NotNull(message = "User ID is required")
    private Long userId;

    /**
     * Amount to confirm from pending to available balance.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Description/reason for confirmation.
     */
    private String description;
}
