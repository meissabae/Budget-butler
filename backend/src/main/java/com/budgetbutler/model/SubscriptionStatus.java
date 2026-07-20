package com.budgetbutler.model;

/**
 * TRIAL   = within the first 20 days after registering - full access, free.
 * ACTIVE  = user paid via Stripe - full access.
 * EXPIRED = trial ended and no active payment - locked out of the "personality" features.
 */
public enum SubscriptionStatus {
    TRIAL,
    ACTIVE,
    EXPIRED
}
