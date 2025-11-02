package com.cashbee.application.usecase;

import com.cashbee.application.dto.response.AffiliateOrderItemResponse;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.common.exception.NotFoundException;
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
 * Use Case: Get a specific order by ID with all its items.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
public class GetOrderByIdUseCase {

    private final AffiliateOrderRepository orderRepository;
    private final AffiliateOrderItemRepository orderItemRepository;
    private final AffiliatePlatformRepository platformRepository;

    /**
     * Execute use case: get order by ID.
     *
     * @param orderId order ID
     * @return order with items
     * @throws NotFoundException if order not found
     */
    @Transactional(readOnly = true)
    public AffiliateOrderResponse execute(Long orderId) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found with ID: " + orderId));

        return mapToResponse(order);
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
