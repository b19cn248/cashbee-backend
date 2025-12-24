package com.cashbee.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for checking first login status.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckFirstLoginResponse {

    /**
     * Whether this is the user's first login.
     * - true: User has never logged in before (first time)
     * - false: User has logged in at least once before
     */
    private boolean firstLogin;
}
