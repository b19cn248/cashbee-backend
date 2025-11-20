package com.cashbee.application.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing one row in the Excel batch transfer file.
 *
 * Maps to columns in VPBank template:
 * - STT (Row Number)
 * - Số Tài Khoản (Account Number)
 * - Tên Tài Khoản (Account Name)
 * - Số Tiền (Amount)
 * - Ngân Hàng Hưởng (Bank)
 * - Nội Dung (Remark)
 *
 * @author CashBee Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTransferRow {

    /**
     * STT (Row number).
     * Auto-incremented: 1, 2, 3, ...
     */
    private Integer stt;

    /**
     * Số tài khoản ngân hàng.
     */
    private String accountNumber;

    /**
     * Tên chủ tài khoản.
     */
    private String accountName;

    /**
     * Số tiền cần chuyển (balance của user).
     */
    private BigDecimal amount;

    /**
     * Ngân hàng hưởng.
     * Example: "VPBANK", "VCB", "ACB"
     */
    private String bankName;

    /**
     * Nội dung chuyển khoản.
     * Example: "Hoan tien CashBee 11/2025"
     */
    private String remark;

    /**
     * User ID (metadata, không xuất ra Excel).
     * Used for logging/debugging only.
     */
    private Long userId;
}
