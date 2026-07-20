package com.budgetbutler.dto;

// What GET /api/subscription/status returns to the frontend.
public record SubscriptionStatusResponse(
        String billingStatus,      // "TRIAL", "ACTIVE", or "EXPIRED"
        String currentTier,        // "FREE", "PLUS", or "PREMIUM" - the effective tier right now
        int trialDaysRemaining
) {
}
