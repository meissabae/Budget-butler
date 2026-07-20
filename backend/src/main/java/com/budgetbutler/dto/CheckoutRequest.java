package com.budgetbutler.dto;

// What Angular sends to POST /api/subscription/create-checkout-session
public record CheckoutRequest(
        String tier,     // "PLUS" or "PREMIUM"
        String interval  // "MONTHLY" or "ANNUAL"
) {
}
