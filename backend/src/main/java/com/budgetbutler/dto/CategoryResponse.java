package com.budgetbutler.dto;

import com.budgetbutler.model.Category;

public record CategoryResponse(Long id, String name, Double monthlyLimit, Long walletId, String walletName) {

    public static CategoryResponse from(Category category) {
        Long walletId = category.getWallet() != null ? category.getWallet().getId() : null;
        String walletName = category.getWallet() != null ? category.getWallet().getName() : null;
        return new CategoryResponse(category.getId(), category.getName(), category.getMonthlyLimit(), walletId, walletName);
    }
}
