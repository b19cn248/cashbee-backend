package com.cashbee.application.usecase;

import com.cashbee.application.dto.request.CreateAffiliateOrderRequest;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.domain.model.AffiliateOrder;
import com.cashbee.domain.model.AffiliateOrderItem;
import com.cashbee.domain.repository.AffiliateOrderItemRepository;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import com.cashbee.domain.repository.AffiliatePlatformRepository;
import com.cashbee.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Use Case: Create a new affiliate order.
 * Used for admin operations and import functionality.
 *
 * @author CashBee Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAffiliateOrderUseCase {

    private final AffiliateOrderRepository orderRepository;
    private final AffiliateOrderItemRepository orderItemRepository;
    private final AffiliatePlatformRepository platformRepository;
    private final UserRepository userRepository;
    private final GetOrderByIdUseCase getOrderByIdUseCase;

    /**
     * Execute use case: create new order.
     *
     * @param request create order request
     * @return created order with items
     */
    @Transactional
    public AffiliateOrderResponse execute(CreateAffiliateOrderRequest request) {
        log.info("Creating new affiliate order: {}", request.getOrderId());

        // Validate platform exists
        platformRepository.findById(request.getPlatformId())
            .orElseThrow(() -> new BadRequestException("Platform not found with ID: " + request.getPlatformId()));

        // Validate user exists
        userRepository.findById(request.getUserId())
            .orElseThrow(() -> new BadRequestException("User not found with ID: " + request.getUserId()));

        // Check if order ID already exists
        if (orderRepository.existsByOrderId(request.getOrderId())) {
            throw new BadRequestException("Order already exists with order ID: " + request.getOrderId());
        }

        // Create domain model from request
        var order = AffiliateOrder.builder()
            .platformId(request.getPlatformId())
            .userId(request.getUserId())
            .clickId(request.getClickId())
            .orderId(request.getOrderId())
            .orderStatus(request.getOrderStatus())
            .productName(request.getProductName())
            .productPrice(request.getProductPrice())
            .commissionAmount(request.getCommissionAmount())
            .currency(request.getCurrency())
            .orderTime(request.getOrderTime())
            .confirmTime(request.getConfirmTime())
            .paidTime(request.getPaidTime())
            .source(request.getSource())
            .importBatchId(request.getImportBatchId())
            .build();

        // Validate order
        order.validate();

        // Save order
        var savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        // Create and save items if provided
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            var items = request.getItems().stream()
                .map(itemReq -> AffiliateOrderItem.builder()
                    .orderId(savedOrder.getId())
                    .itemId(itemReq.getItemId())
                    .itemName(itemReq.getItemName())
                    .quantity(itemReq.getQuantity())
                    .actualAmount(itemReq.getActualAmount())
                    .itemCommission(itemReq.getItemCommission())
                    .shopId(itemReq.getShopId())
                    .shopName(itemReq.getShopName())
                    .categoryLv1(itemReq.getCategoryLv1())
                    .categoryLv2(itemReq.getCategoryLv2())
                    .categoryLv3(itemReq.getCategoryLv3())
                    .imgUrl(itemReq.getImgUrl())
                    .brandCommissionRate(itemReq.getBrandCommissionRate())
                    .platformCommissionRate(itemReq.getPlatformCommissionRate())
                    .build())
                .collect(Collectors.toList());

            // Validate all items
            items.forEach(AffiliateOrderItem::validate);

            // Save all items
            orderItemRepository.saveAll(items);
            log.info("Saved {} items for order {}", items.size(), savedOrder.getId());
        }

        // Return complete order with items
        return getOrderByIdUseCase.execute(savedOrder.getId());
    }
}
