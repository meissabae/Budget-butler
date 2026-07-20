package com.budgetbutler.dto;

import com.budgetbutler.model.RecurringTransaction;

public record RecurringTransactionResponse(
        Long id,
        String description,
        Double amount,
        Integer dayOfMonth,
        boolean active,
        Long categoryId,
        String categoryName
) {
    public static RecurringTransactionResponse from(RecurringTransaction r) {
        Long categoryId = r.getCategory() != null ? r.getCategory().getId() : null;
        String categoryName = r.getCategory() != null ? r.getCategory().getName() : null;
        return new RecurringTransactionResponse(r.getId(), r.getDescription(), r.getAmount(),
                r.getDayOfMonth(), r.isActive(), categoryId, categoryName);
    }
}
