package com.cashbee.application.usecase.order;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.order.GetUserOrdersQuery;
import com.cashbee.application.dto.response.AffiliateOrderItemResponse;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.domain.model.AffiliateOrderItem;
import com.cashbee.domain.repository.AffiliateOrderItemRepository;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for getting user's orders with pagination.
 *
 * This use case handles:
 * - Fetching orders for a specific user
 * - Filtering by order status (optional)
 * - Including order items in response
 * - Pagination support
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserOrdersUseCase {

    private final AffiliateOrderRepository orderRepository;
    private final AffiliateOrderItemRepository orderItemRepository;
    private final AffiliatePlatformRepository platformRepository;

    /**
     * Execute the use case to get user's orders.
     *
     * @param query Query parameters including userId, status filter, pagination
     * @return Paginated response with orders and their items
     * @throws IllegalArgumentException if query is null or invalid
     */
    @Transactional(readOnly = true)
    public PageResponse<AffiliateOrderResponse> execute(GetUserOrdersQuery query) {
        // Validate query
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        // Validate and normalize query parameters
        query.validate();

        Long userId = query.getUserId();
        log.info("Getting orders for user {} (page: {}, size: {}, status: {})",
                userId, query.getPage(), query.getSize(), query.getStatus());

        // Create pageable with sorting by orderTime DESC (most recent first)
        Pageable pageable = PageRequest.of(
                query.getPage(),
                query.getSize(),
                Sort.by(Sort.Direction.DESC, "orderTime")
        );

        // Fetch orders based on filter
        Page<AffiliateOrder> orderPage;
        if (query.getStatus() != null) {
            // Filter by status
            orderPage = orderRepository.findByUserIdAndStatus(userId, query.getStatus(), pageable);
            log.debug("Found {} orders with status {} for user {}",
                    orderPage.getTotalElements(), query.getStatus(), userId);
        } else {
            // Get all orders
            orderPage = orderRepository.findByUserId(userId, pageable);
            log.debug("Found {} orders for user {}", orderPage.getTotalElements(), userId);
        }

        // Map to response DTOs with items
        List<AffiliateOrderResponse> orderResponses = orderPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("Returning {} orders for user {} (page {} of {})",
                orderResponses.size(), userId, query.getPage(), orderPage.getTotalPages());

        // Build paginated response
        return PageResponse.of(
                orderResponses,
                query.getPage(),
                query.getSize(),
                orderPage.getTotalElements()
        );
    }

    /**
     * Map domain order to response DTO, including items.
     */
    private AffiliateOrderResponse mapToResponse(AffiliateOrder order) {
        // Fetch items for this order
        List<AffiliateOrderItem> items = orderItemRepository.findByOrderId(order.getId());

        // Map items to response DTOs
        List<AffiliateOrderItemResponse> itemResponses = items.stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        // Get platform name (optional - for display)
        String platformName = null;
        if (order.getPlatformId() != null) {
            platformName = platformRepository.findById(order.getPlatformId())
                    .map(p -> p.getName())
                    .orElse(null);
        }

        return AffiliateOrderResponse.builder()
                .id(order.getId())
                .platformId(order.getPlatformId())
                .platformName(platformName)
                .userId(order.getUserId())
                .clickId(order.getClickId())
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus())
                .productName(order.getProductName())
                .productPrice(order.getProductPrice())
                .commissionAmount(order.getCommissionAmount())
                .currency(order.getCurrency())
                .orderTime(order.getOrderTime())
                .confirmTime(order.getConfirmTime())
                .paidTime(order.getPaidTime())
                .source(order.getSource())
                .importBatchId(order.getImportBatchId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .canReceiveCashback(order.canReceiveCashback())
                .build();
    }

    /**
     * Map domain order item to response DTO.
     */
    private AffiliateOrderItemResponse mapItemToResponse(AffiliateOrderItem item) {
        return AffiliateOrderItemResponse.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .itemId(item.getItemId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .actualAmount(item.getActualAmount())
                .itemCommission(item.getItemCommission())
                .shopId(item.getShopId())
                .shopName(item.getShopName())
                .categoryLv1(item.getCategoryLv1())
                .categoryLv2(item.getCategoryLv2())
                .categoryLv3(item.getCategoryLv3())
                .imgUrl(item.getImgUrl())
                .brandCommissionRate(item.getBrandCommissionRate())
                .platformCommissionRate(item.getPlatformCommissionRate())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
