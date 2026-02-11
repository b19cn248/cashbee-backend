package com.cashbee.application.usecase.voucher;

import com.cashbee.application.dto.voucher.BatchImportResponse;
import com.cashbee.application.dto.voucher.CreateVoucherCommand;
import com.cashbee.application.util.affiliate.ShopeeAffiliateLinkBuilder;
import com.cashbee.application.util.affiliate.ShopeeUrlParser;
import com.cashbee.domain.enums.VoucherCategory;
import com.cashbee.domain.enums.VoucherStatus;
import com.cashbee.domain.model.AffiliatePlatform;
import com.cashbee.domain.model.PromotionVoucher;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.PromotionVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Use case for batch importing vouchers.
 *
 * Features:
 * - Process multiple vouchers in a single request
 * - Auto-generate affiliate links
 * - Track success/failure for each voucher
 * - Update existing vouchers if duplicate code found
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchImportVouchersUseCase {

    private final PromotionVoucherRepository voucherRepository;
    private final AffiliatePlatformRepository platformRepository;
    private final ShopeeUrlParser shopeeUrlParser;
    private final ShopeeAffiliateLinkBuilder shopeeAffiliateLinkBuilder;

    /**
     * Execute batch import of vouchers.
     *
     * @param commands List of voucher commands to import
     * @return BatchImportResponse with import statistics
     */
    @Transactional
    public BatchImportResponse execute(List<CreateVoucherCommand> commands) {
        log.info("Starting batch import of {} vouchers", commands.size());

        BatchImportResponse response = BatchImportResponse.builder()
            .totalReceived(commands.size())
            .created(0)
            .updated(0)
            .failed(0)
            .build();

        // Get affiliate platform configuration once
        AffiliatePlatform shopeePlatform = platformRepository.findByCode("shopee").orElse(null);

        for (int i = 0; i < commands.size(); i++) {
            CreateVoucherCommand command = commands.get(i);
            try {
                boolean isUpdate = processVoucher(command, shopeePlatform);
                if (isUpdate) {
                    response.setUpdated(response.getUpdated() + 1);
                } else {
                    response.setCreated(response.getCreated() + 1);
                }
            } catch (Exception e) {
                log.warn("Failed to import voucher at index {}: {}", i, e.getMessage());
                response.setFailed(response.getFailed() + 1);
                response.addError(i, e.getMessage());
            }
        }

        log.info("Batch import completed. Created: {}, Updated: {}, Failed: {}",
            response.getCreated(), response.getUpdated(), response.getFailed());

        return response;
    }

    /**
     * Process a single voucher - create new or update existing.
     *
     * @param command Voucher command
     * @param platform Affiliate platform for link building
     * @return true if voucher was updated, false if created
     */
    private boolean processVoucher(CreateVoucherCommand command, AffiliatePlatform platform) {
        // Validate required fields
        if (command.getTitle() == null || command.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (command.getDiscountType() == null) {
            throw new IllegalArgumentException("Discount type is required");
        }

        // Build affiliate link if original link is provided
        String affiliateLink = null;
        if (command.getOriginalLink() != null && !command.getOriginalLink().isBlank()) {
            affiliateLink = buildAffiliateLink(command.getOriginalLink(), platform);
        }

        LocalDateTime now = LocalDateTime.now();

        // Check if voucher with same code exists (for update)
        if (command.getCode() != null && !command.getCode().isBlank()) {
            Optional<PromotionVoucher> existingVoucher = voucherRepository.findByCode(command.getCode());
            if (existingVoucher.isPresent()) {
                // Update existing voucher
                PromotionVoucher voucher = existingVoucher.get();
                updateVoucherFromCommand(voucher, command, affiliateLink, now);
                voucherRepository.save(voucher);
                log.debug("Updated existing voucher with code: {}", command.getCode());
                return true;
            }
        }

        // Create new voucher
        PromotionVoucher voucher = PromotionVoucher.builder()
            .code(command.getCode())
            .title(command.getTitle())
            .description(command.getDescription())
            .discountType(command.getDiscountType())
            .discountValue(command.getDiscountValue())
            .maxDiscount(command.getMaxDiscount())
            .minOrderValue(command.getMinOrderValue())
            .originalLink(command.getOriginalLink())
            .affiliateLink(affiliateLink)
            .voucherSaveLink(command.getVoucherSaveLink())
            .category(command.getCategory() != null ? command.getCategory() : VoucherCategory.OTHER)
            .platform(command.getPlatform() != null ? command.getPlatform() : "SHOPEE")
            .validFrom(command.getValidFrom())
            .validUntil(command.getValidUntil())
            .validTimeSlot(command.getValidTimeSlot())
            .status(VoucherStatus.ACTIVE)
            .viewCount(0)
            .clickCount(0)
            .createdAt(now)
            .updatedAt(now)
            .build();

        voucherRepository.save(voucher);
        log.debug("Created new voucher: {}", command.getTitle());
        return false;
    }

    /**
     * Update existing voucher from command.
     *
     * @param voucher Existing voucher to update
     * @param command New data
     * @param affiliateLink Generated affiliate link
     * @param now Current timestamp
     */
    private void updateVoucherFromCommand(
        PromotionVoucher voucher,
        CreateVoucherCommand command,
        String affiliateLink,
        LocalDateTime now
    ) {
        voucher.setTitle(command.getTitle());
        voucher.setDescription(command.getDescription());
        voucher.setDiscountType(command.getDiscountType());
        voucher.setDiscountValue(command.getDiscountValue());
        voucher.setMaxDiscount(command.getMaxDiscount());
        voucher.setMinOrderValue(command.getMinOrderValue());
        voucher.setOriginalLink(command.getOriginalLink());

        // Only update affiliate link if we generated a new one
        if (affiliateLink != null) {
            voucher.setAffiliateLink(affiliateLink);
        }

        voucher.setVoucherSaveLink(command.getVoucherSaveLink());

        if (command.getCategory() != null) {
            voucher.setCategory(command.getCategory());
        }
        if (command.getPlatform() != null) {
            voucher.setPlatform(command.getPlatform());
        }

        voucher.setValidFrom(command.getValidFrom());
        voucher.setValidUntil(command.getValidUntil());
        voucher.setValidTimeSlot(command.getValidTimeSlot());
        voucher.setUpdatedAt(now);
    }

    /**
     * Build affiliate link from original Shopee URL.
     *
     * @param originalLink Original Shopee link
     * @param platform Affiliate platform configuration
     * @return Affiliate link or null if building fails
     */
    private String buildAffiliateLink(String originalLink, AffiliatePlatform platform) {
        if (platform == null || platform.getAffiliateId() == null) {
            log.debug("Shopee affiliate platform not configured, skipping affiliate link generation");
            return null;
        }

        try {
            // Parse URL (handles shortened links automatically)
            ShopeeUrlParser.ParsedShopeeUrl parsedUrl = shopeeUrlParser.parse(originalLink);
            String urlForAffiliate = parsedUrl.getUrlForAffiliateLink();

            // Build affiliate link
            String trackingCode = "voucher_" + System.currentTimeMillis();
            return shopeeAffiliateLinkBuilder.build(
                urlForAffiliate,
                platform.getAffiliateId(),
                trackingCode
            );
        } catch (Exception e) {
            log.debug("Failed to build affiliate link from URL: {}. Error: {}", originalLink, e.getMessage());
            return null;
        }
    }
}
