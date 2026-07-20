package com.budgetbutler.dto;

import java.util.List;

public record StreakResponse(
        int currentStreak,
        int longestStreak,
        boolean isNewRecord,
        String streakMessage,
        List<BadgeDto> badges
) {
}
