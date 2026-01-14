package com.cashbee.application.dto.voucher;

import com.cashbee.domain.enums.VoucherStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Command DTO for updating voucher status.
 *
 * @author CashBee Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVoucherStatusCommand {

    /**
     * New status for the voucher.
     */
    @NotNull(message = "Status is required")
    private VoucherStatus status;
}
