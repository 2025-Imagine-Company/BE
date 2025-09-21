package com.example.AudIon.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private PageInfo pageInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int page;           // Current page number (0-based)
        private int size;           // Page size
        private int totalPages;     // Total number of pages
        private long totalElements; // Total number of elements
        private boolean hasNext;    // Whether there is a next page
        private boolean hasPrevious; // Whether there is a previous page
        private boolean isFirst;    // Whether this is the first page
        private boolean isLast;     // Whether this is the last page
    }

    public static <T> PagedResponse<T> of(List<T> content, org.springframework.data.domain.Page<?> page) {
        return PagedResponse.<T>builder()
                .content(content)
                .pageInfo(PageInfo.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalPages(page.getTotalPages())
                        .totalElements(page.getTotalElements())
                        .hasNext(page.hasNext())
                        .hasPrevious(page.hasPrevious())
                        .isFirst(page.isFirst())
                        .isLast(page.isLast())
                        .build())
                .build();
    }
}