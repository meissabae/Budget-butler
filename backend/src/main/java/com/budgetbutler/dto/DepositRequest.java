package com.budgetbutler.dto;

// What Angular sends to POST /api/wallets/{id}/deposit - adding money anytime, not just salary
public record DepositRequest(Double amount, String note) {
}
