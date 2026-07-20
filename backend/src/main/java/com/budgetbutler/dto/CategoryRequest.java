package com.budgetbutler.dto;

// What Angular sends to POST/PUT /api/categories
public record CategoryRequest(String name, Double monthlyLimit, Long walletId) {
}
