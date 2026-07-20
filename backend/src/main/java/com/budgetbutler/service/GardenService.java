package com.budgetbutler.service;

import com.budgetbutler.dto.GardenResponse;
import com.budgetbutler.model.*;
import com.budgetbutler.repository.CategoryRepository;
import com.budgetbutler.repository.DreamRepository;
import com.budgetbutler.repository.GardenRepository;
import com.budgetbutler.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Turns the user's overall financial behavior into a simple, emotional visual: a growing
 * (or wilting) garden. The score (0-100) is built from 4 equally-weighted signals:
 *
 * 1) Category discipline - what fraction of budget categories are NOT currently over their limit
 * 2) Savings           - how much of this month's total budget was actually saved
 * 3) Dream progress     - how close the user's dream is to fully funded
 * 4) Consistency streak - consecutive recent days without overspending
 *
 * The result is saved back to the Garden table each time, so next time we can tell
 * whether things are improving or declining (used to pick the right status message).
 */
@Service
public class GardenService {

    @Autowired
    private GardenRepository gardenRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DreamRepository dreamRepository;

    public GardenResponse getGardenStatus(User user) {
        Garden garden = gardenRepository.findByOwner(user).orElse(new Garden(user));
        double previousGrowth = garden.getGrowthPercentage();

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);

        List<Category> categories = categoryRepository.findByOwner(user);
        double totalBudget = categories.stream().mapToDouble(Category::getMonthlyLimit).sum();
        double thisMonthTotal = sumTransactions(user, thisMonth.atDay(1), today);

        double categoryScore = calculateCategoryScore(user, categories, thisMonth, today);
        double savingsScore = calculateSavingsScore(totalBudget, thisMonthTotal);
        double dreamScore = calculateDreamScore(user);
        double streakScore = calculateStreakScore(user, categories, today);

        double growthPercentage = categoryScore + savingsScore + dreamScore + streakScore; // each maxes at 25 -> total 0-100
        growthPercentage = Math.max(0, Math.min(100, growthPercentage));

        int level = growthPercentageToLevel(growthPercentage);
        String overBudgetCategory = findWorstOverBudgetCategory(user, categories, thisMonth, today);
        String statusMessage = buildStatusMessage(growthPercentage, previousGrowth, overBudgetCategory);

        garden.setOwner(user);
        garden.setLevel(level);
        garden.setGrowthPercentage(growthPercentage);
        garden.setStatusMessage(statusMessage);
        garden.setLastUpdated(java.time.LocalDateTime.now());
        gardenRepository.save(garden);

        return toResponse(garden);
    }

    // ------------------------------------------------------------------
    // Scoring factors (each returns a value from 0 to 25)
    // ------------------------------------------------------------------

    /** 25 points if every category is within budget this month, scaled down as more go over. */
    private double calculateCategoryScore(User user, List<Category> categories, YearMonth thisMonth, LocalDate today) {
        if (categories.isEmpty()) {
            return 12.5; // neutral score - nothing to judge yet
        }
        long withinBudgetCount = categories.stream()
                .filter(c -> sumTransactionsForCategory(user, c, thisMonth.atDay(1), today) <= c.getMonthlyLimit())
                .count();
        return (withinBudgetCount / (double) categories.size()) * 25.0;
    }

    /** 25 points for saving 100% of the budget, scaling down to 0 as spending approaches/exceeds the budget. */
    private double calculateSavingsScore(double totalBudget, double thisMonthTotal) {
        if (totalBudget <= 0) {
            return 12.5; // no budget set up yet - neutral score
        }
        double savingsRatio = (totalBudget - thisMonthTotal) / totalBudget; // 1.0 = spent nothing, 0 = spent it all, negative = over
        savingsRatio = Math.max(0, Math.min(1, savingsRatio));
        return savingsRatio * 25.0;
    }

    /** 25 points for fully-funded dreams (averaged across all of them); neutral half-credit if none are set. */
    private double calculateDreamScore(User user) {
        List<Dream> dreams = dreamRepository.findByOwner(user);
        if (dreams.isEmpty()) {
            return 12.5;
        }

        double totalProgress = 0;
        int countedDreams = 0;
        for (Dream dream : dreams) {
            if (dream.getTargetAmount() == null || dream.getTargetAmount() <= 0) continue;
            double progress = dream.getSavedAmount() / dream.getTargetAmount();
            totalProgress += Math.max(0, Math.min(1, progress));
            countedDreams++;
        }

        if (countedDreams == 0) return 12.5;
        double averageProgress = totalProgress / countedDreams;
        return averageProgress * 25.0;
    }

    /** 25 points for 10+ consecutive "clean" days (spending under the average daily budget). */
    private double calculateStreakScore(User user, List<Category> categories, LocalDate today) {
        int streak = calculateConsecutiveCleanDays(user, categories, today);
        return Math.min(streak / 10.0, 1.0) * 25.0;
    }

    private int calculateConsecutiveCleanDays(User user, List<Category> categories, LocalDate today) {
        double totalBudget = categories.stream().mapToDouble(Category::getMonthlyLimit).sum();
        if (totalBudget <= 0) {
            return 0;
        }
        double averageDailyBudget = totalBudget / 30.0;

        int streak = 0;
        LocalDate day = today.minusDays(1); // start checking from yesterday
        for (int i = 0; i < 30; i++) { // cap the lookback at 30 days
            double spentThatDay = sumTransactions(user, day, day);
            if (spentThatDay <= averageDailyBudget) {
                streak++;
                day = day.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    // ------------------------------------------------------------------
    // Turning the score into a level + message
    // ------------------------------------------------------------------

    private int growthPercentageToLevel(double growthPercentage) {
        if (growthPercentage <= 20) return 1;
        if (growthPercentage <= 40) return 2;
        if (growthPercentage <= 60) return 3;
        if (growthPercentage <= 80) return 4;
        return 5;
    }

    private String findWorstOverBudgetCategory(User user, List<Category> categories, YearMonth thisMonth, LocalDate today) {
        String worstCategory = null;
        double worstOverspend = 0;
        for (Category c : categories) {
            double spent = sumTransactionsForCategory(user, c, thisMonth.atDay(1), today);
            double overspend = spent - c.getMonthlyLimit();
            if (overspend > worstOverspend) {
                worstOverspend = overspend;
                worstCategory = c.getName();
            }
        }
        return worstCategory; // null if nothing is currently over budget
    }

    private String buildStatusMessage(double growthPercentage, double previousGrowth, String overBudgetCategory) {
        if (overBudgetCategory != null) {
            return String.format("Some leaves are falling. %s spending exceeded the limit.", overBudgetCategory);
        }
        if (growthPercentage < previousGrowth - 5) {
            return "Your garden is wilting a little - it's been drifting away from your dream goals lately.";
        }
        if (growthPercentage >= 81) {
            return "Your garden is a thriving, beautiful oasis. This is what financial peace looks like!";
        }
        if (growthPercentage >= 61) {
            return "Your garden is flourishing because you've respected your budget this week.";
        }
        if (growthPercentage >= 41) {
            return "Your garden is growing beautifully. Keep going.";
        }
        if (growthPercentage >= 21) {
            return "Your garden is sprouting - a little more discipline will help it grow faster.";
        }
        return "Your garden is just a seed right now. Every dollar you save helps it grow.";
    }

    private GardenResponse toResponse(Garden garden) {
        String stageName;
        String stageEmoji;
        switch (garden.getLevel()) {
            case 1 -> { stageName = "Seed"; stageEmoji = "🌱"; }
            case 2 -> { stageName = "Sprout"; stageEmoji = "🌿"; }
            case 3 -> { stageName = "Young Tree"; stageEmoji = "🌳"; }
            case 4 -> { stageName = "Flowering Tree"; stageEmoji = "🌸"; }
            default -> { stageName = "Beautiful Garden"; stageEmoji = "🏡"; }
        }
        return new GardenResponse(garden.getLevel(), stageName, stageEmoji, garden.getGrowthPercentage(), garden.getStatusMessage());
    }

    // ------------------------------------------------------------------
    // Small reusable helpers (same pattern as DashboardService)
    // ------------------------------------------------------------------

    private double sumTransactions(User user, LocalDate start, LocalDate end) {
        return transactionRepository.findByOwnerAndDateBetween(user, start, end)
                .stream().mapToDouble(Transaction::getAmount).sum();
    }

    private double sumTransactionsForCategory(User user, Category category, LocalDate start, LocalDate end) {
        return transactionRepository.findByOwnerAndCategoryAndDateBetween(user, category, start, end)
                .stream().mapToDouble(Transaction::getAmount).sum();
    }
}
