package com.cashbee.application.dto.common;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * Provides pagination metadata along with the actual content.
 * This is a framework-independent alternative to Spring Data's Page interface,
 * maintaining clean hexagonal architecture.
 *
 * @param <T> Type of content items
 * @author CashBee Team
 */
@Getter
@Builder
@ToString
public class PageResponse<T> {

    /**
     * The actual content items for this page.
     */
    private final List<T> content;

    /**
     * Current page number (0-indexed).
     */
    private final int page;

    /**
     * Number of items per page.
     */
    private final int size;

    /**
     * Total number of elements across all pages.
     */
    private final long totalElements;

    /**
     * Total number of pages.
     */
    private final int totalPages;

    /**
     * Whether this is the first page.
     */
    private final boolean first;

    /**
     * Whether this is the last page.
     */
    private final boolean last;

    /**
     * Create a PageResponse from content list and pagination metadata.
     *
     * @param content List of items for this page
     * @param page Current page number (0-indexed)
     * @param size Page size
     * @param totalElements Total elements across all pages
     * @param <T> Type of content items
     * @return PageResponse with computed metadata
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean isFirst = page == 0;
        boolean isLast = page >= totalPages - 1;

        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(isFirst)
                .last(isLast)
                .build();
    }
}
