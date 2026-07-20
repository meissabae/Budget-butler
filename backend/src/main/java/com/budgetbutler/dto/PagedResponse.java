package com.budgetbutler.dto;

import java.util.List;

// A generic "page of results" shape - reused anywhere we need pagination instead of
// dumping an entire (potentially huge) table into one JSON response.
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
