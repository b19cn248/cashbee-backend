package com.cashbee.application.usecase.voucher;

import com.cashbee.application.dto.voucher.VoucherTrackingResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.PromotionVoucher;
import com.cashbee.domain.repository.PromotionVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for tracking voucher clicks.
 *
 * Increments the click count when a user clicks on a voucher link.
 * Returns the affiliate link for redirect.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackVoucherClickUseCase {

    private final PromotionVoucherRepository voucherRepository;

    /**
     * Execute use case to track a voucher click.
     *
     * @param voucherId Voucher ID
     * @return VoucherTrackingResponse with updated click count and affiliate link
     * @throws NotFoundException if voucher not found
     */
    @Transactional
    public VoucherTrackingResponse execute(Long voucherId) {
        log.info("Tracking click for voucher ID: {}", voucherId);

        // Get voucher to return affiliate link
        PromotionVoucher voucher = voucherRepository.findById(voucherId)
            .orElseThrow(() -> {
                log.warn("Voucher not found with ID: {}", voucherId);
                return new NotFoundException("Voucher not found with id: " + voucherId);
            });

        // Increment click count
        int updatedRows = voucherRepository.incrementClickCount(voucherId);

        if (updatedRows == 0) {
            log.warn("Failed to increment click count for voucher ID: {}", voucherId);
        }

        // Get updated voucher with new click count
        Integer clickCount = voucherRepository.findById(voucherId)
            .map(v -> v.getClickCount())
            .orElse(voucher.getClickCount() + 1);

        log.info("Click tracked for voucher ID: {}, new click count: {}", voucherId, clickCount);

        // Determine link to return (prefer affiliate link, fallback to original/save link)
        String linkToReturn = voucher.getAffiliateLink();
        if (linkToReturn == null || linkToReturn.isBlank()) {
            linkToReturn = voucher.getOriginalLink();
        }
        if (linkToReturn == null || linkToReturn.isBlank()) {
            linkToReturn = voucher.getVoucherSaveLink();
        }

        return VoucherTrackingResponse.builder()
            .voucherId(voucherId)
            .clickCount(clickCount)
            .affiliateLink(linkToReturn)
            .build();
    }
}
