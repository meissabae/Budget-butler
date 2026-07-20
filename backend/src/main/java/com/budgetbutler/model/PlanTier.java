package com.budgetbutler.model;

/**
 * FREE    = "dry" tracking only - categories, transactions, dream (no messages/features)
 * PLUS    = adds the lighter personality features (Daily Title, Twin Comparison message, Pace Analysis)
 * PREMIUM = adds everything else (Dream Protector, Memory Lane, Talking Reports,
 *           Payday Patterns, Time Cost, Dream Garden)
 */
public enum PlanTier {
    FREE,
    PLUS,
    PREMIUM
}
