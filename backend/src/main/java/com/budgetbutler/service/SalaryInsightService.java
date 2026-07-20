package com.budgetbutler.service;

import com.budgetbutler.model.Transaction;
import com.budgetbutler.model.User;
import com.budgetbutler.model.UserSettings;
import com.budgetbutler.repository.TransactionRepository;
import com.budgetbutler.repository.UserSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Looks for a pattern that's very common but rarely pointed out: people tend to spend
 * differently right after getting paid than during the rest of the month. This service
 * compares "the week right after payday" against "the rest of the month" across the
 * user's transaction history, and turns the result into plain-English insights.
 *
 * Everything here is recalculated live from transaction history each time - there's no
 * separate "insights" table to keep in sync.
 */
@Service
public class SalaryInsightService {

    private static final int POST_PAYDAY_WINDOW_DAYS = 7; // "first week after payday"
    private static final int MONTHS_OF_HISTORY_TO_CHECK = 3;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /** Returns a short list of human-readable insight sentences, or an empty list if there isn't enough data yet. */
    public List<String> buildInsights(User user) {
        Optional<UserSettings> settingsOpt = userSettingsRepository.findByOwner(user);
        if (settingsOpt.isEmpty() || settingsOpt.get().getSalaryPaymentDay() == null) {
            return List.of(); // user hasn't set up their payday yet - nothing to analyze
        }
        int paymentDay = settingsOpt.get().getSalaryPaymentDay();

        List<Transaction> allTransactions = transactionRepository.findByOwner(user);
        if (allTransactions.size() < 5) {
            return List.of(); // not enough data yet for a meaningful pattern
        }

        List<String> insights = new ArrayList<>();

        addAccelerationInsight(insights, allTransactions, paymentDay);
        addTopCategoryAfterPaydayInsight(insights, allTransactions, paymentDay);

        return insights;
    }

    // ------------------------------------------------------------------
    // Insight A: "You usually spend X% more during the first week after payday"
    // ------------------------------------------------------------------
    private void addAccelerationInsight(List<String> insights, List<Transaction> transactions, int paymentDay) {
        double postPaydayTotal = 0;
        int postPaydayDays = 0;
        double restOfMonthTotal = 0;
        int restOfMonthDays = 0;

        // Look at each of the last few months and classify every day as
        // "within the post-payday window" or "the rest of the month".
        YearMonth current = YearMonth.now();
        for (int i = 0; i < MONTHS_OF_HISTORY_TO_CHECK; i++) {
            YearMonth month = current.minusMonths(i);
            LocalDate payday = safePaydayDate(month, paymentDay);
            LocalDate windowEnd = payday.plusDays(POST_PAYDAY_WINDOW_DAYS - 1);

            for (Transaction t : transactions) {
                YearMonth txMonth = YearMonth.from(t.getDate());
                if (!txMonth.equals(month)) continue;

                if (!t.getDate().isBefore(payday) && !t.getDate().isAfter(windowEnd)) {
                    postPaydayTotal += t.getAmount();
                } else {
                    restOfMonthTotal += t.getAmount();
                }
            }
            postPaydayDays += POST_PAYDAY_WINDOW_DAYS;
            restOfMonthDays += Math.max(month.lengthOfMonth() - POST_PAYDAY_WINDOW_DAYS, 1);
        }

        if (postPaydayTotal == 0 || restOfMonthTotal == 0) {
            return; // not enough spread of data to compare fairly
        }

        double postPaydayDailyAvg = postPaydayTotal / postPaydayDays;
        double restDailyAvg = restOfMonthTotal / restOfMonthDays;

        double percentDifference = ((postPaydayDailyAvg - restDailyAvg) / restDailyAvg) * 100.0;

        if (percentDifference > 10) {
            insights.add(String.format(
                    "You usually spend %.0f%% more per day during the first week after payday. " +
                    "Your spending tends to accelerate right after you get paid.",
                    percentDifference));
        } else if (percentDifference < -10) {
            insights.add(String.format(
                    "Interesting - you actually spend %.0f%% LESS per day right after payday than during " +
                    "the rest of the month. Disciplined!",
                    Math.abs(percentDifference)));
        } else {
            insights.add("Your spending is fairly steady throughout the month - no big payday spike detected.");
        }
    }

    // ------------------------------------------------------------------
    // Insight B: "Most of your Restaurants expenses happen within 3 days after payday"
    // ------------------------------------------------------------------
    private void addTopCategoryAfterPaydayInsight(List<String> insights, List<Transaction> transactions, int paymentDay) {
        int shortWindowDays = 3;

        // For every category, track: total spent in that category overall,
        // and how much of that happened within the short post-payday window.
        Map<String, Double> totalByCategory = new HashMap<>();
        Map<String, Double> postPaydayByCategory = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.getCategory() == null) continue;
            String categoryName = t.getCategory().getName();
            totalByCategory.merge(categoryName, t.getAmount(), Double::sum);

            YearMonth txMonth = YearMonth.from(t.getDate());
            LocalDate payday = safePaydayDate(txMonth, paymentDay);
            long daysSincePayday = ChronoUnit.DAYS.between(payday, t.getDate());

            if (daysSincePayday >= 0 && daysSincePayday < shortWindowDays) {
                postPaydayByCategory.merge(categoryName, t.getAmount(), Double::sum);
            }
        }

        String topCategory = null;
        double topPercent = 0;

        for (String category : totalByCategory.keySet()) {
            double total = totalByCategory.get(category);
            double postPayday = postPaydayByCategory.getOrDefault(category, 0.0);
            if (total <= 0) continue;

            double percent = (postPayday / total) * 100.0;
            if (percent > topPercent && postPayday > 0) {
                topPercent = percent;
                topCategory = category;
            }
        }

        if (topCategory != null && topPercent >= 40) {
            insights.add(String.format(
                    "%.0f%% of your '%s' expenses happen within %d days after payday. " +
                    "That looks like a celebration habit!",
                    topPercent, topCategory, shortWindowDays));
        }
    }

    /** Clamps the payment day to the month's actual length (e.g. day 31 in February -> Feb 28/29). */
    private LocalDate safePaydayDate(YearMonth month, int paymentDay) {
        int safeDay = Math.min(paymentDay, month.lengthOfMonth());
        return month.atDay(safeDay);
    }
}
