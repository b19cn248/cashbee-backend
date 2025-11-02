package com.cashbee.application.usecase;

import com.cashbee.application.dto.request.UpdateOrderStatusRequest;
import com.cashbee.application.dto.response.AffiliateOrderResponse;
import com.cashbee.common.exception.BadRequestException;
import com.cashbee.common.exception.NotFoundException;
import com.cashbee.domain.enums.OrderStatus;
import com.cashbee.domain.repository.AffiliateOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Update order status.
 * Used by admin to approve/reject/cancel orders.
 *
 * @author CashBee Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

    private final AffiliateOrderRepository orderRepository;
    private final GetOrderByIdUseCase getOrderByIdUseCase;

    /**
     * Execute use case: update order status.
     *
     * @param orderId order ID
     * @param request status update request
     * @return updated order
     */
    @Transactional
    public AffiliateOrderResponse execute(Long orderId, UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to: {}", orderId, request.getNewStatus());

        // Find order
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found with ID: " + orderId));

        // Validate status transition
        validateStatusTransition(order.getOrderStatus(), request.getNewStatus());

        // Update status using domain methods
        switch (request.getNewStatus()) {
            case APPROVED -> order.approve();
            case PAID -> order.markAsPaid();
            case CANCELLED -> order.cancel();
            case REJECTED -> order.reject();
            case PENDING -> throw new BadRequestException("Cannot change status back to PENDING");
            default -> throw new BadRequestException("Invalid status: " + request.getNewStatus());
        }

        // Save updated order
        orderRepository.save(order);
        log.info("Order {} status updated successfully", orderId);

        // Return updated order with items
        return getOrderByIdUseCase.execute(orderId);
    }

    /**
     * Validate status transition.
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new BadRequestException("Order is already in " + currentStatus + " status");
        }

        // Business rules for status transitions
        switch (currentStatus) {
            case PENDING -> {
                // PENDING can go to: APPROVED, REJECTED, CANCELLED
                if (newStatus != OrderStatus.APPROVED
                    && newStatus != OrderStatus.REJECTED
                    && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException(
                        "Cannot change status from PENDING to " + newStatus +
                        ". Valid transitions: APPROVED, REJECTED, CANCELLED"
                    );
                }
            }
            case APPROVED -> {
                // APPROVED can go to: PAID, CANCELLED
                if (newStatus != OrderStatus.PAID && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException(
                        "Cannot change status from APPROVED to " + newStatus +
                        ". Valid transitions: PAID, CANCELLED"
                    );
                }
            }
            case PAID, CANCELLED, REJECTED -> {
                // Final states - cannot transition
                throw new BadRequestException(
                    "Cannot change status from " + currentStatus + " (final state)"
                );
            }
        }
    }
}
