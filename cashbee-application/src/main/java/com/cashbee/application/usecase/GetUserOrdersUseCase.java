package com.cashbee.application.usecase;

import com.cashbee.application.dto.response.AffiliateOrderItemResponse;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.domain.model.AffiliateOrderItem;
import com.cashbee.domain.repository.AffiliateOrderItemRepository;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case: Get all orders for a specific user.
 * This is the KEY use case for users to view their orders.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
public class GetUserOrdersUseCase {

    private final AffiliateOrderRepository orderRepository;
    private final AffiliateOrderItemRepository orderItemRepository;
    private final AffiliatePlatformRepository platformRepository;

    /**
     * Execute use case: get all orders for a user.
     *
     * @param userId user ID (from JWT token)
     * @return list of orders with items
     */
    @Transactional(readOnly = true)
    public List<AffiliateOrderResponse> execute(Long userId) {
        // Fetch all orders for this user
        var orders = orderRepository.findByUserId(userId);

        // Map to response DTOs
        return orders.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Map domain model to response DTO.
     */
    private AffiliateOrderResponse mapToResponse(AffiliateOrder order) {
        // Fetch items for this order
        var items = orderItemRepository.findByOrderId(order.getId());

        // Get platform name
        var platformName = platformRepository.findById(order.getPlatformId())
            .map(p -> p.getName())
            .orElse("Unknown Platform");

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
            .items(mapItems(items))
            .totalItems(items.size())
            .canReceiveCashback(order.canReceiveCashback())
            .build();
    }

    /**
     * Map items to response DTOs.
     */
    private List<AffiliateOrderItemResponse> mapItems(List<AffiliateOrderItem> items) {
        return items.stream()
            .map(item -> AffiliateOrderItemResponse.builder()
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
                .createdAt(item.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }
}
