package com.cashbee.application.usecase.affiliate;

import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.model.AffiliateClick;
import com.cashbee.domain.repository.AffiliateClickRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for handling click redirect tracking.
 *
 * When user clicks on tracking link:
 * 1. Find AffiliateClick by click ID
 * 2. Mark click as CLICKED
 * 3. Record timestamp
 * 4. Return tracking URL for redirect
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HandleClickRedirectUseCase {

    private final AffiliateClickRepository clickRepository;

    /**
     * Execute use case to handle click redirect.
     *
     * @param clickId Click ID from URL path
     * @return Tracking URL to redirect to (the actual Shopee affiliate URL)
     * @throws NotFoundException if click not found
     */
    @Transactional
    public String execute(Long clickId) {
        log.info("UseCase: Handling click redirect for click ID: {}", clickId);

        // Find the click
        AffiliateClick click = clickRepository.findById(clickId)
            .orElseThrow(() -> {
                log.error("UseCase: Click not found with ID: {}", clickId);
                return new NotFoundException("Tracking link not found or expired");
            });

        // Mark as clicked (if not already clicked)
        if (!click.getStatus().name().equals("CLICKED") &&
            !click.getStatus().name().equals("MATCHED")) {
            click.markAsClicked();
            clickRepository.save(click);
            log.info("UseCase: Marked click {} as CLICKED at {}",
                clickId, click.getClickedAt());
        } else {
            log.info("UseCase: Click {} already tracked (status: {})",
                clickId, click.getStatus());
        }

        // Return the tracking URL for redirect
        String redirectUrl = click.getTrackingUrl();
        log.info("UseCase: Redirecting click {} to: {}", clickId, redirectUrl);

        return redirectUrl;
    }

    /**
     * Execute use case by tracking code (alternative lookup method).
     *
     * @param trackingCode Tracking code (e.g., CB1_100_20251101160530)
     * @return Tracking URL to redirect to
     * @throws NotFoundException if click not found
     */
    @Transactional
    public String executeByTrackingCode(String trackingCode) {
        log.info("UseCase: Handling click redirect for tracking code: {}", trackingCode);

        // Find the click by tracking code
        AffiliateClick click = clickRepository.findByTrackingCode(trackingCode)
            .orElseThrow(() -> {
                log.error("UseCase: Click not found with tracking code: {}", trackingCode);
                return new NotFoundException("Tracking link not found or expired");
            });

        // Mark as clicked
        if (!click.getStatus().name().equals("CLICKED") &&
            !click.getStatus().name().equals("MATCHED")) {
            click.markAsClicked();
            clickRepository.save(click);
            log.info("UseCase: Marked click {} (code: {}) as CLICKED",
                click.getId(), trackingCode);
        }

        // Return the tracking URL for redirect
        return click.getTrackingUrl();
    }
}
