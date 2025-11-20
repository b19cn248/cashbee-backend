package com.cashbee.application.dto.bank;

import lombok.*;

/**
 * Bank Response DTO.
 * Used to return bank information to clients.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankResponse {

    /**
     * Bank ID.
     */
    private Long id;

    /**
     * Bank code (e.g., VPBANK, ACB).
     */
    private String bankCode;

    /**
     * Full bank name.
     */
    private String bankName;

    /**
     * Short name for display (e.g., VPBank, ACB).
     */
    private String shortName;

    /**
     * Whether this bank is active.
     */
    private Boolean isActive;
}
