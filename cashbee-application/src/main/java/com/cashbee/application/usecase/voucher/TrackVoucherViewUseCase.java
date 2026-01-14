package com.cashbee.application.usecase.voucher;

import com.cashbee.application.dto.voucher.VoucherTrackingResponse;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.repository.PromotionVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for tracking voucher views.
 *
 * Increments the view count when a user views a voucher.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackVoucherViewUseCase {

    private final PromotionVoucherRepository voucherRepository;

    /**
     * Execute use case to track a voucher view.
     *
     * @param voucherId Voucher ID
     * @return VoucherTrackingResponse with updated view count
     * @throws NotFoundException if voucher not found
     */
    @Transactional
    public VoucherTrackingResponse execute(Long voucherId) {
        log.info("Tracking view for voucher ID: {}", voucherId);

        // Check if voucher exists
        if (!voucherRepository.existsById(voucherId)) {
            log.warn("Voucher not found with ID: {}", voucherId);
            throw new NotFoundException("Voucher not found with id: " + voucherId);
        }

        // Increment view count
        int updatedRows = voucherRepository.incrementViewCount(voucherId);

        if (updatedRows == 0) {
            log.warn("Failed to increment view count for voucher ID: {}", voucherId);
            throw new NotFoundException("Voucher not found with id: " + voucherId);
        }

        // Get updated view count
        Integer viewCount = voucherRepository.findById(voucherId)
            .map(v -> v.getViewCount())
            .orElse(0);

        log.info("View tracked for voucher ID: {}, new view count: {}", voucherId, viewCount);

        return VoucherTrackingResponse.builder()
            .voucherId(voucherId)
            .viewCount(viewCount)
            .build();
    }
}
