package com.cashbee.infrastructure.external.shoppingtietkiem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response from STK {@code POST /api/auth/login}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShoppingTietKiemLoginResponse {

    private String message;
    private Boolean restored;
    private String accessToken;
    private String refreshToken;
}
