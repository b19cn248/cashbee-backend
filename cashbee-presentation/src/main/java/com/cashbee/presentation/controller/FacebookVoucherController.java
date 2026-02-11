package com.cashbee.presentation.controller;

import com.cashbee.application.dto.affiliate.ConvertFacebookVoucherRequest;
import com.cashbee.application.dto.affiliate.ConvertFacebookVoucherResponse;
import com.cashbee.application.usecase.affiliate.ConvertFacebookVoucherLinkUseCase;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facebook-voucher")
@RequiredArgsConstructor
@Tag(name = "Facebook Voucher", description = "Convert Shopee links to get Facebook-exclusive vouchers (20-25% off)")
public class FacebookVoucherController {

    private static final Logger log = LoggerFactory.getLogger(FacebookVoucherController.class);

    private final ConvertFacebookVoucherLinkUseCase convertFacebookVoucherLinkUseCase;

    @PostMapping("/convert")
    @Operation(
        summary = "Convert Shopee URL for Facebook voucher",
        description = "Convert Shopee product URL to affiliate link. Requires authentication."
    )
    public ResponseEntity<ApiResponse<ConvertFacebookVoucherResponse>> convertLink(
        @Valid @RequestBody ConvertFacebookVoucherRequest request,
        @AuthenticationPrincipal Jwt jwt) {

        log.info("API: Converting Shopee URL for Facebook voucher - url: {}", request.getShopeeUrl());

        ConvertFacebookVoucherResponse response = convertFacebookVoucherLinkUseCase.execute(request);

        log.info("API: Successfully converted URL. affiliateLink: {}", response.getAffiliateLink());

        return ResponseEntity.ok(
            ApiResponse.success(response, "Link converted successfully. Copy and paste on Facebook, then click from Facebook to get voucher.")
        );
    }
}
