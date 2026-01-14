package com.cashbee.presentation.controller;

import com.cashbee.application.dto.common.PageResponse;
import com.cashbee.application.dto.invoice.GetUserInvoicesQuery;
import com.cashbee.application.dto.invoice.InvoiceDetailResponse;
import com.cashbee.application.dto.invoice.InvoiceSummaryResponse;
import com.cashbee.application.dto.invoice.PaymentInvoiceResponse;
import com.cashbee.application.usecase.invoice.GetInvoiceDetailUseCase;
import com.cashbee.application.usecase.invoice.GetInvoiceWithOrderDetailsUseCase;
import com.cashbee.application.usecase.invoice.GetUserInvoicesUseCase;
import com.cashbee.application.util.SecurityUtils;
import com.cashbee.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Payment Invoice operations.
 *
 * Endpoints:
 * - GET /api/invoices/me - Get current user's invoices (paginated)
 * - GET /api/invoices/{invoiceId} - Get invoice detail by ID
 * - GET /api/invoices/by-number/{invoiceNumber} - Get invoice by invoice number
 *
 * Admin Endpoints:
 * - GET /api/invoices/admin/{invoiceId} - Get any invoice (admin access)
 *
 * @author CashBee Team
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Payment Invoices", description = "Payment invoice management endpoints")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final GetUserInvoicesUseCase getUserInvoicesUseCase;
    private final GetInvoiceDetailUseCase getInvoiceDetailUseCase;
    private final GetInvoiceWithOrderDetailsUseCase getInvoiceWithOrderDetailsUseCase;
    private final SecurityUtils securityUtils;

    /**
     * Get current user's payment invoices (paginated).
     *
     * This endpoint returns a list of payment invoices for the authenticated user.
     * Each invoice represents a completed batch transfer payment.
     *
     * Usage (Frontend):
     * <pre>
     * // Get first page of invoices (10 per page)
     * const response = await fetch('/api/invoices/me?page=0&size=10', {
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * const result = response.data;
     * // result.content - array of invoice summaries
     * // result.totalElements - total number of invoices
     * // result.totalPages - total number of pages
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 10, max: 50)
     * @return Paginated list of invoice summaries
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user's invoices",
        description = "Get paginated list of payment invoices for the current authenticated user"
    )
    public ResponseEntity<ApiResponse<PageResponse<InvoiceSummaryResponse>>> getCurrentUserInvoices(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)")
            @RequestParam(defaultValue = "10") int size) {

        log.info("API: Getting invoices for current user, page={}, size={}", page, size);

        // Extract userId from JWT token
        Long userId = securityUtils.getCurrentUserId(jwt);

        // Validate page size
        if (size > 50) {
            size = 50;
        }

        GetUserInvoicesQuery query = GetUserInvoicesQuery.builder()
                .userId(userId)
                .page(page)
                .size(size)
                .build();

        PageResponse<InvoiceSummaryResponse> invoices = getUserInvoicesUseCase.execute(query);

        log.info("API: Found {} invoices for user {}", invoices.getTotalElements(), userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(invoices));
    }

    /**
     * Get invoice detail by invoice ID.
     *
     * Returns full details of a specific invoice including platform breakdown.
     * Only the owner of the invoice can access it.
     *
     * Usage (Frontend):
     * <pre>
     * const response = await fetch('/api/invoices/123', {
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * const invoice = response.data;
     * // invoice.invoiceNumber - "INV-20260104-00001"
     * // invoice.amount - 50000
     * // invoice.shopeeAmount, lazadaAmount, tikiAmount, tiktokAmount, otherAmount
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param invoiceId Invoice ID
     * @return Invoice detail
     */
    @GetMapping("/{invoiceId}")
    @Operation(
        summary = "Get invoice detail by ID",
        description = "Get full details of a payment invoice by ID (only accessible by owner)"
    )
    public ResponseEntity<ApiResponse<PaymentInvoiceResponse>> getInvoiceById(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Invoice ID")
            @PathVariable Long invoiceId) {

        log.info("API: Getting invoice detail: invoiceId={}", invoiceId);

        // Extract userId from JWT token
        Long userId = securityUtils.getCurrentUserId(jwt);

        PaymentInvoiceResponse invoice = getInvoiceDetailUseCase.execute(invoiceId, userId);

        log.info("API: Invoice {} retrieved for user {}", invoice.getInvoiceNumber(), userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(invoice));
    }

    /**
     * Get invoice detail with order breakdown.
     *
     * This endpoint returns a "supermarket receipt" style view of an invoice,
     * showing every order and item that contributed to the total cashback.
     * Perfect for users who want to see exactly what they're being paid for.
     *
     * Response includes:
     * - Invoice metadata (number, status, timestamps)
     * - Summary totals (orders, items, amounts, rates)
     * - Bank transfer info
     * - List of orders grouped by order code, each with item details
     * - Calculation explanation for transparency
     *
     * Usage (Frontend):
     * <pre>
     * const response = await fetch('/api/invoices/123/details', {
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * const detail = response.data;
     * // detail.summary.totalOrders - total number of orders
     * // detail.summary.totalCashback - total cashback amount
     * // detail.orders - array of orders with item breakdown
     * // detail.orders[0].items - array of items in the order
     * // detail.calculation - explanation of how cashback was calculated
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param invoiceId Invoice ID
     * @return Detailed invoice with order breakdown
     */
    @GetMapping("/{invoiceId}/details")
    @Operation(
        summary = "Get invoice detail with order breakdown",
        description = "Get detailed invoice showing every order and item that contributed to the cashback payment"
    )
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> getInvoiceWithOrderDetails(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Invoice ID")
            @PathVariable Long invoiceId) {

        log.info("API: Getting invoice with order details: invoiceId={}", invoiceId);

        // Extract userId from JWT token
        Long userId = securityUtils.getCurrentUserId(jwt);

        InvoiceDetailResponse detail = getInvoiceWithOrderDetailsUseCase.execute(invoiceId, userId);

        log.info("API: Invoice {} details retrieved with {} orders for user {}",
                detail.getInvoiceNumber(), detail.getOrders().size(), userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(detail));
    }

    /**
     * Get invoice detail by invoice number.
     *
     * Alternative endpoint to get invoice by human-readable invoice number
     * instead of internal ID. Only the owner of the invoice can access it.
     *
     * Usage (Frontend):
     * <pre>
     * const response = await fetch('/api/invoices/by-number/INV-20260104-00001', {
     *   headers: { 'Authorization': `Bearer ${token}` }
     * });
     * const invoice = response.data;
     * </pre>
     *
     * @param jwt JWT token (auto-injected by Spring Security)
     * @param invoiceNumber Invoice number (e.g., "INV-20260104-00001")
     * @return Invoice detail
     */
    @GetMapping("/by-number/{invoiceNumber}")
    @Operation(
        summary = "Get invoice detail by invoice number",
        description = "Get full details of a payment invoice by invoice number (only accessible by owner)"
    )
    public ResponseEntity<ApiResponse<PaymentInvoiceResponse>> getInvoiceByNumber(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Invoice number (e.g., INV-20260104-00001)")
            @PathVariable String invoiceNumber) {

        log.info("API: Getting invoice detail by number: {}", invoiceNumber);

        // Extract userId from JWT token
        Long userId = securityUtils.getCurrentUserId(jwt);

        PaymentInvoiceResponse invoice = getInvoiceDetailUseCase.executeByInvoiceNumber(invoiceNumber, userId);

        log.info("API: Invoice {} retrieved for user {}", invoiceNumber, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(invoice));
    }

    /**
     * Get any invoice by ID (Admin access).
     *
     * This endpoint is for admin use only and bypasses ownership validation.
     *
     * @param invoiceId Invoice ID
     * @return Invoice detail
     */
    @GetMapping("/admin/{invoiceId}")
    @Operation(
        summary = "Get any invoice by ID (Admin)",
        description = "Admin endpoint to get any payment invoice without ownership check"
    )
    public ResponseEntity<ApiResponse<PaymentInvoiceResponse>> getInvoiceForAdmin(
            @Parameter(description = "Invoice ID")
            @PathVariable Long invoiceId) {

        log.info("API: Admin getting invoice detail: invoiceId={}", invoiceId);

        PaymentInvoiceResponse invoice = getInvoiceDetailUseCase.executeForAdmin(invoiceId);

        log.info("API: Admin retrieved invoice {}", invoice.getInvoiceNumber());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(invoice));
    }
}
