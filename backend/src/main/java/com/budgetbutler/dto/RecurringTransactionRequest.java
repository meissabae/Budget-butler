package com.budgetbutler.dto;

// What Angular sends to POST/PUT /api/recurring-transactions
public record RecurringTransactionRequest(
        String description,
        Double amount,
        Integer dayOfMonth,
        Long categoryId,
        Boolean active
) {
}
