package com.budgetbutler.dto;

public record GardenResponse(
        int level,
        String stageName,     // "Seed", "Sprout", "Young Tree", "Flowering Tree", "Beautiful Garden"
        String stageEmoji,    // 🌱 🌿 🌳 🌸 🏡
        double growthPercentage,
        String statusMessage
) {
}
