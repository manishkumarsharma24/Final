package com.shopverse.shared;

import java.util.List;

/**
 * Ch07-01: Generic paged response for paginated API endpoints.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PagedResponse<>(
                content, page, size, totalElements, totalPages,
                page == 0, page >= totalPages - 1
        );
    }
}
