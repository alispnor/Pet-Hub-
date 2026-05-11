package com.alispnor.pethub.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wrapper padrão de paginação retornado pelos controllers.
 * Evita expor a estrutura interna do {@link Page} do Spring Data.
 */
public record PageableResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageableResponse<T> of(Page<T> page) {
        return new PageableResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
