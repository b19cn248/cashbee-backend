package com.cashbee.application.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command DTO để update thông tin profile của user.
 * <p>
 * Cho phép update các thông tin:
 * - phone: Số điện thoại
 * - referredBy: Mã giới thiệu của người đã giới thiệu user này
 * - accountNumber: Số tài khoản ngân hàng
 * - accountName: Tên chủ tài khoản
 * - bankName: Tên ngân hàng
 * - bankCode: Mã ngân hàng
 * <p>
 * Tất cả các trường đều optional - chỉ update nếu được cung cấp.
 * <p>
 * Lưu ý: referredBy chỉ có thể set 1 lần duy nhất. Nếu user đã có referredBy thì không thể thay đổi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileCommand {

    /**
     * Số điện thoại (định dạng Việt Nam: 0XXXXXXXXX).
     * Optional - có thể null hoặc rỗng.
     * Nếu có giá trị thì phải đúng định dạng.
     */
    @Pattern(regexp = "^(0\\d{9})?$", message = "Phone must be 10 digits starting with 0")
    private String phone;

    /**
     * Mã giới thiệu của người đã giới thiệu user này.
     * Optional - có thể null hoặc rỗng.
     * Nếu có giá trị thì phải đúng định dạng: CB + 6 ký tự alphanumeric (ví dụ: CB4F7A9K)
     * <p>
     * Lưu ý: Chỉ có thể set 1 lần duy nhất.
     */
    @Pattern(
            regexp = "^(CB[A-Z0-9]{6})?$",
            message = "Mã giới thiệu phải có định dạng: CB + 6 ký tự chữ và số"
    )
    private String referredBy;

    /**
     * Số tài khoản ngân hàng.
     * Optional - có thể null hoặc rỗng.
     * Tối đa 50 ký tự.
     */
    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;

    /**
     * Tên chủ tài khoản (theo đúng CMND/CCCD).
     * Optional - có thể null hoặc rỗng.
     * Tối đa 200 ký tự.
     */
    @Size(max = 200, message = "Account name must not exceed 200 characters")
    private String accountName;

    /**
     * Tên ngân hàng (VD: "Ngân hàng TMCP Việt Nam Thịnh Vượng").
     * Optional - có thể null hoặc rỗng.
     * Tối đa 100 ký tự.
     */
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    /**
     * Mã ngân hàng (VD: "VPBANK", "VCB", "ACB").
     * Optional - có thể null hoặc rỗng.
     * Tối đa 20 ký tự.
     */
    @Size(max = 20, message = "Bank code must not exceed 20 characters")
    private String bankCode;

    /**
     * Kiểm tra xem có thông tin bank account nào được cung cấp không.
     *
     * @return true nếu có ít nhất 1 field bank account được cung cấp
     */
    public boolean hasBankAccountInfo() {
        return isNotBlank(accountNumber) || isNotBlank(accountName)
                || isNotBlank(bankName) || isNotBlank(bankCode);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
