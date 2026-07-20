package com.budgetbutler.service;

import com.budgetbutler.model.PlanTier;
import com.budgetbutler.model.SubscriptionStatus;
import com.budgetbutler.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Centralizes all the "what can this user access right now?" logic in ONE place.
 * Every other part of the app (DashboardService, controllers, etc.) should ask THIS class
 * instead of re-implementing the trial/tier math themselves.
 */
@Service
public class SubscriptionService {

    private static final int TRIAL_LENGTH_DAYS = 20;

    /**
     * The single source of truth for "what tier of features does this user see right now?"
     * - During the trial, everyone gets full PREMIUM access, regardless of what they'll pay for later.
     * - Once the trial ends, it drops to whatever they've actually paid for (FREE by default).
     * - An ACTIVE subscription always wins and returns the tier they bought (PLUS or PREMIUM).
     */
    public PlanTier getEffectiveTier(User user) {
        if (user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE) {
            return user.getPlanTier(); // PLUS or PREMIUM, whichever they're paying for
        }
        if (user.getSubscriptionStatus() == SubscriptionStatus.TRIAL && getTrialDaysRemaining(user) > 0) {
            return PlanTier.PREMIUM; // full access during the trial
        }
        return PlanTier.FREE; // trial over, nothing purchased
    }

    public boolean hasAtLeast(User user, PlanTier requiredTier) {
        return getEffectiveTier(user).ordinal() >= requiredTier.ordinal();
    }

    /** How many days are left in the free trial (0 if it's over or they're not on trial). */
    public int getTrialDaysRemaining(User user) {
        if (user.getTrialStartDate() == null) {
            return 0;
        }
        long daysElapsed = ChronoUnit.DAYS.between(user.getTrialStartDate(), LocalDateTime.now());
        long remaining = TRIAL_LENGTH_DAYS - daysElapsed;
        return (int) Math.max(remaining, 0);
    }

    public static int getTrialLengthDays() {
        return TRIAL_LENGTH_DAYS;
    }
}
