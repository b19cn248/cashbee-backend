package com.cashbee.application.dto.user;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command DTO để update số điện thoại và mã giới thiệu.
 * <p>
 * API đơn giản hơn UpdateUserCommand - chỉ cho phép update 2 trường: - phone: Số điện thoại -
 * referredBy: Mã giới thiệu của người đã giới thiệu user này
 * <p>
 * Cả 2 trường đều optional - chỉ update nếu được cung cấp.
 * <p>
 * Lưu ý: referredBy chỉ có thể set 1 lần duy nhất. Nếu user đã có referredBy thì không thể thay
 * đổi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePhoneAndReferralCommand {

  /**
   * Số điện thoại (định dạng Việt Nam: 0XXXXXXXXX). Optional - có thể null hoặc rỗng. Nếu có giá
   * trị thì phải đúng định dạng.
   */
  @Pattern(regexp = "^(0\\d{9})?$", message = "Phone must be 10 digits starting with 0")
  private String phone;

  /**
   * Mã giới thiệu của người đã giới thiệu user này. Optional - có thể null hoặc rỗng.
   * Nếu có giá trị thì phải đúng định dạng: CB + 6 ký tự alphanumeric (ví dụ: CB4F7A9K)
   * <p>
   * Lưu ý: Chỉ có thể set 1 lần duy nhất.
   */
  @Pattern(
      regexp = "^(CB[A-Z0-9]{6})?$",
      message = "Mã giới thiệu phải có định dạng: CB + 6 ký tự chữ và số"
  )
  private String referredBy;
}
