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
 * For VietinBank template, additional field vietinbankCode is used
 * instead of bankName for the bank column.
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
     * Ngân hàng hưởng (tên đầy đủ).
     * Example: "Ngân hàng TMCP Việt Nam Thịnh Vượng"
     */
    private String bankName;

    /**
     * Mã ngân hàng (viết tắt).
     * Example: "VPBANK", "VCB", "ACB"
     * Used for VPBank template.
     */
    private String bankCode;

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

    /**
     * Mã ngân hàng theo chuẩn VietinBank (8 chữ số).
     * Example: "01309001" (VPBank), "01202001" (BIDV)
     * Special: "VietinBank" for internal VietinBank transfer
     *
     * Used only for VietinBank template.
     */
    private String vietinbankCode;

    /**
     * Mã ngân hàng theo chuẩn VPBank (số nguyên).
     * Example: 28 (Techcombank), 1 (VPBank)
     *
     * Used only for VPBank template (column BANKID).
     */
    private Integer vpbankId;
}
