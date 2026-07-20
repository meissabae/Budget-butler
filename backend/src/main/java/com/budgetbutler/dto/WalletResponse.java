package com.budgetbutler.dto;

import com.budgetbutler.model.Wallet;

public record WalletResponse(Long id, String name, Double balance) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getName(), wallet.getBalance());
    }
}
