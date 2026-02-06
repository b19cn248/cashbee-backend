package com.cashbee.application.usecase.affiliate;

import com.cashbee.application.dto.affiliate.ConvertFacebookVoucherRequest;
import com.cashbee.application.dto.affiliate.ConvertFacebookVoucherResponse;
import com.cashbee.application.util.affiliate.DaoshopeeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConvertFacebookVoucherLinkUseCase {

    private final DaoshopeeClient daoshopeeClient;

    private static final String FB_LINK = "https://www.facebook.com/groups/570967567029047/posts/2060163244776131/";

    public ConvertFacebookVoucherResponse execute(ConvertFacebookVoucherRequest request) {
        log.info("Converting Shopee URL for Facebook voucher - url: {}", request.getShopeeUrl());

        String affiliateLink = daoshopeeClient.generateAffiliateLink(request.getShopeeUrl(), "");
        log.info("Generated affiliate link: {}", affiliateLink);

        return ConvertFacebookVoucherResponse.builder()
            .affiliateLink(affiliateLink)
            .fbLink(FB_LINK)
            .build();
    }
}
