package com.budgetbutler.dto;

public record BadgeDto(
        String name,
        String emoji,
        String description,
        boolean earned
) {
}
