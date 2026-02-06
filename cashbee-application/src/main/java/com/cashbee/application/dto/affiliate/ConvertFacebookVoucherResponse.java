package com.cashbee.application.dto.affiliate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertFacebookVoucherResponse {

    private String affiliateLink;

    private String fbLink;
}
