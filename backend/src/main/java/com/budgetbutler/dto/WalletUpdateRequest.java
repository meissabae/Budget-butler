package com.budgetbutler.dto;

// What Angular sends to PUT /api/wallets/{id} (rename) and POST /api/wallets/{id}/deposit
public record WalletUpdateRequest(String name) {
}
