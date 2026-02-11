package com.cashbee.application.dto.user;

import com.cashbee.domain.enums.UserLevel;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Command DTO for updating user level (admin operation).
 *
 * Used by admin to change user's tier level manually.
 * This is useful for:
 * - Promoting special customers to DIAMOND level
 * - Adjusting user levels for promotions
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserLevelCommand {

    /**
     * New user level to set.
     * Must be one of: NORMAL, VIP, SUPER, DIAMOND
     */
    @NotNull(message = "User level is required")
    private UserLevel userLevel;
}
