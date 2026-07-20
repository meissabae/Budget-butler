package com.budgetbutler.service;

import com.budgetbutler.dto.BadgeDto;
import com.budgetbutler.dto.StreakResponse;
import com.budgetbutler.model.*;
import com.budgetbutler.repository.CategoryRepository;
import com.budgetbutler.repository.DreamRepository;
import com.budgetbutler.repository.TransactionRepository;
import com.budgetbutler.repository.UserStreakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Streaks play on a well-known psychological effect: people hate losing progress they've
 * already built up (loss aversion) - the same mechanic that keeps people opening Duolingo
 * every day. The CURRENT streak is recalculated live every time (same pattern as the rest
 * of this app); the LONGEST streak is the one thing we persist, so a broken streak doesn't
 * erase the user's personal best.
 */
@Service
public class StreakService {

    @Autowired
    private UserStreakRepository userStreakRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DreamRepository dreamRepository;

    public StreakResponse getStreakStatus(User user) {
        LocalDate today = LocalDate.now();
        List<Category> categories = categoryRepository.findByOwner(user);

        int currentStreak = calculateCurrentStreak(user, categories, today);

        UserStreak streakRecord = userStreakRepository.findByOwner(user).orElse(new UserStreak(user));
        boolean isNewRecord = currentStreak > streakRecord.getLongestStreak();
        if (isNewRecord) {
            streakRecord.setLongestStreak(currentStreak);
        }
        streakRecord.setOwner(user);
        streakRecord.setUpdatedAt(java.time.LocalDateTime.now());
        userStreakRepository.save(streakRecord);

        String message = buildStreakMessage(currentStreak, streakRecord.getLongestStreak(), isNewRecord);
        List<BadgeDto> badges = buildBadges(user, categories, currentStreak, streakRecord.getLongestStreak(), today);

        return new StreakResponse(currentStreak, streakRecord.getLongestStreak(), isNewRecord, message, badges);
    }

    /**
     * Counts consecutive recent days (walking backward from yesterday) where total spending
     * stayed under the user's average daily budget. Stops at the first day that broke it.
     * (Same approach used by GardenService's "consistency" scoring factor.)
     */
    private int calculateCurrentStreak(User user, List<Category> categories, LocalDate today) {
        double totalBudget = categories.stream().mapToDouble(Category::getMonthlyLimit).sum();
        if (totalBudget <= 0) {
            return 0;
        }
        double averageDailyBudget = totalBudget / 30.0;

        int streak = 0;
        LocalDate day = today.minusDays(1); // start checking from yesterday
        for (int i = 0; i < 60; i++) { // cap the lookback so this can't run forever
            double spentThatDay = transactionRepository.findByOwnerAndDateBetween(user, day, day)
                    .stream().mapToDouble(Transaction::getAmount).sum();
            if (spentThatDay <= averageDailyBudget) {
                streak++;
                day = day.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private String buildStreakMessage(int currentStreak, int longestStreak, boolean isNewRecord) {
        if (currentStreak == 0 && longestStreak > 0) {
            return String.format(
                    "Your streak broke. Your personal best was %d day(s) - time to start climbing back up!",
                    longestStreak);
        }
        if (isNewRecord && currentStreak > 0) {
            return String.format("New personal best! %d consecutive day(s) under budget. Keep it going!", currentStreak);
        }
        if (currentStreak >= 7) {
            return String.format("%d days strong! Your discipline is showing.", currentStreak);
        }
        if (currentStreak > 0) {
            return String.format("%d day(s) under budget in a row. Every day adds up!", currentStreak);
        }
        return "No active streak yet - stay under your average daily budget today to start one!";
    }

    private List<BadgeDto> buildBadges(User user, List<Category> categories, int currentStreak, int longestStreak, LocalDate today) {
        List<BadgeDto> badges = new ArrayList<>();

        badges.add(new BadgeDto("Disciplined Week", "🏅", "Reach a 7-day streak", currentStreak >= 7 || longestStreak >= 7));
        badges.add(new BadgeDto("Iron Will", "💪", "Reach a 30-day streak", currentStreak >= 30 || longestStreak >= 30));

        // Dream Guardian - at least 50% of the way to ANY of the user's dream goals
        boolean dreamGuardian = false;
        List<Dream> dreams = dreamRepository.findByOwner(user);
        for (Dream dream : dreams) {
            if (dream.getTargetAmount() != null && dream.getTargetAmount() > 0) {
                double progress = dream.getSavedAmount() / dream.getTargetAmount();
                if (progress >= 0.5) {
                    dreamGuardian = true;
                    break;
                }
            }
        }
        badges.add(new BadgeDto("Dream Guardian", "🛡️", "Reach 50% of a dream goal", dreamGuardian));

        // Golden Month - the most recently COMPLETED month stayed within total budget
        boolean goldenMonth = false;
        double totalBudget = categories.stream().mapToDouble(Category::getMonthlyLimit).sum();
        if (totalBudget > 0) {
            YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
            double lastMonthSpent = transactionRepository
                    .findByOwnerAndDateBetween(user, lastMonth.atDay(1), lastMonth.atEndOfMonth())
                    .stream().mapToDouble(Transaction::getAmount).sum();
            goldenMonth = lastMonthSpent > 0 && lastMonthSpent <= totalBudget;
        }
        badges.add(new BadgeDto("Golden Month", "🏆", "Finish a full month within budget", goldenMonth));

        return badges;
    }
}
