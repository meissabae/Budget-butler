package com.budgetbutler.service;

import com.budgetbutler.dto.DashboardResponse;
import com.budgetbutler.dto.WalletResponse;
import com.budgetbutler.model.Category;
import com.budgetbutler.model.Dream;
import com.budgetbutler.model.PlanTier;
import com.budgetbutler.model.Transaction;
import com.budgetbutler.model.User;
import com.budgetbutler.repository.CategoryRepository;
import com.budgetbutler.repository.DreamRepository;
import com.budgetbutler.repository.TransactionRepository;
import com.budgetbutler.repository.UserSettingsRepository;
import com.budgetbutler.repository.WalletRepository;
import com.budgetbutler.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This is where all the "personality" of Budget Butler lives.
 * Every method here builds ONE piece of the dashboard.
 * The DashboardController just calls buildDashboard() and returns the result as JSON.
 */
@Service
public class DashboardService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DreamRepository dreamRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SalaryInsightService salaryInsightService;

    @Autowired
    private TimeCostService timeCostService;

    @Autowired
    private GardenService gardenService;

    @Autowired
    private StreakService streakService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    public DashboardResponse buildDashboard() {
        User user = currentUserProvider.getCurrentUser();
        LocalDate today = LocalDate.now();

        DashboardResponse response = new DashboardResponse();

        // Wallets & currency are core money data - always shown, regardless of subscription tier.
        walletService.checkAndCreditSalaryIfDue(user); // auto-fills the salary wallet if payday has arrived
        recurringTransactionService.generateDueTransactions(user); // auto-logs rent/subscriptions if due
        response.setWallets(walletRepository.findByOwner(user).stream().map(WalletResponse::from).toList());
        userSettingsRepository.findByOwner(user).ifPresent(s -> response.setCurrency(s.getCurrency()));
        response.setEmailVerified(user.isEmailVerified());

        // Basic numbers (this month's total, etc.) are ALWAYS available, even on the free tier -
        // only the "personality" messages are gated below.
        buildTwinComparisonNumbers(response, user, today);

        PlanTier tier = subscriptionService.getEffectiveTier(user);
        boolean plusUnlocked = tier == PlanTier.PLUS || tier == PlanTier.PREMIUM;
        boolean premiumUnlocked = tier == PlanTier.PREMIUM;

        response.setCurrentTier(tier.name());
        response.setPlusLocked(!plusUnlocked);
        response.setPremiumLocked(!premiumUnlocked);
        response.setTrialDaysRemaining(subscriptionService.getTrialDaysRemaining(user));
        response.setSalaryInsights(List.of());

        if (plusUnlocked) {
            // --- PLUS-tier features ---
            buildDailyTitle(response, user, today, premiumUnlocked); // time cost line only added at Premium
            buildTwinComparisonMessage(response);
            buildPaceAnalysis(response, user, today);
        } else {
            response.setUpgradeMessage(
                    "Subscribe to Plus to unlock the Daily Title, Twin Comparison commentary, and Pace " +
                    "Analysis. Your basic expense tracking stays free forever.");
        }

        if (premiumUnlocked) {
            // --- PREMIUM-tier features (everything else) ---
            buildDreamProtector(response, user, today);
            buildMemoryLane(response, user, today);
            buildPeriodicReport(response, user, today);
            response.setSalaryInsights(salaryInsightService.buildInsights(user));
            buildTimeCostSummary(response, user, today);
            response.setGardenStatus(gardenService.getGardenStatus(user));
            response.setStreakStatus(streakService.getStreakStatus(user));
        } else if (plusUnlocked) {
            // They can see SOME features (Plus) but not the deeper ones yet.
            response.setPremiumUpsellMessage(
                    "Upgrade to Premium to unlock the Dream Protector, Memory Lane, Talking Reports, " +
                    "Payday Patterns, Time Cost insights, and your Dream Garden.");
        }

        return response;
    }

    // ------------------------------------------------------------------
    // 1) DAILY DYNAMIC TITLE
    // Looks at how much was spent YESTERDAY and compares it to the user's
    // average daily budget (total monthly limits / 30).
    // ------------------------------------------------------------------
    private void buildDailyTitle(DashboardResponse response, User user, LocalDate today, boolean includeTimeCost) {
        LocalDate yesterday = today.minusDays(1);
        double spentYesterday = sumTransactionsBetween(user, yesterday, yesterday);
        double averageDailyBudget = getTotalMonthlyBudget(user) / 30.0;

        // Feature 2 (Time Cost) is a Premium-only feature, so we only weave it in
        // when the user has that tier - Plus subscribers get the plain daily title.
        String timeCostSuffix = "";
        if (includeTimeCost) {
            Double hourlyRate = timeCostService.getHourlyRate(user);
            Double hoursYesterday = timeCostService.calculateTimeCostHours(spentYesterday, hourlyRate);
            if (hoursYesterday != null && hoursYesterday >= 0.5) {
                timeCostSuffix = String.format(" That's about %.1f hour(s) of your work life, by the way.", hoursYesterday);
            }
        }

        if (spentYesterday <= averageDailyBudget) {
            response.setNickname("The Wise Owl");
            response.setDailyTitleMessage(String.format(
                    "Good morning, Wise Owl! You only spent $%.2f yesterday. " +
                    "Your bank account is giving you a slow clap. Keep being boring, it's working!%s",
                    spentYesterday, timeCostSuffix));
        } else {
            response.setNickname("The Credit Card Warrior");
            response.setDailyTitleMessage(String.format(
                    "Hello, Credit Card Warrior! You spent $%.2f yesterday and fought bravely against " +
                    "the 'Add to Cart' button... and lost badly. Your wallet is now in the ICU. Take it easy today, champ.%s",
                    spentYesterday, timeCostSuffix));
        }
    }

    // ------------------------------------------------------------------
    // 2) TWIN COMPARISON
    // Compares total spending this month vs. total spending last month.
    // ------------------------------------------------------------------
    private void buildTwinComparisonNumbers(DashboardResponse response, User user, LocalDate today) {
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth lastMonth = thisMonth.minusMonths(1);

        double thisMonthTotal = sumTransactionsBetween(user, thisMonth.atDay(1), thisMonth.atEndOfMonth());
        double lastMonthTotal = sumTransactionsBetween(user, lastMonth.atDay(1), lastMonth.atEndOfMonth());

        response.setThisMonthTotal(thisMonthTotal);
        response.setLastMonthTotal(lastMonthTotal);
    }

    // The witty commentary on top of the numbers above - this part is premium-gated.
    private void buildTwinComparisonMessage(DashboardResponse response) {
        double thisMonthTotal = response.getThisMonthTotal();
        double lastMonthTotal = response.getLastMonthTotal();

        String message;
        if (thisMonthTotal > lastMonthTotal) {
            message = String.format(
                    "Last month: $%.2f | This month: $%.2f. Dude, what happened to the old responsible you? " +
                    "Did they go on vacation? Let's fix this next month!",
                    lastMonthTotal, thisMonthTotal);
        } else if (thisMonthTotal < lastMonthTotal) {
            message = String.format(
                    "Last month: $%.2f | This month: $%.2f. Look at you go! Future you just sent a thank-you note.",
                    lastMonthTotal, thisMonthTotal);
        } else {
            message = String.format(
                    "Last month: $%.2f | This month: $%.2f. Perfectly consistent. Suspiciously consistent, actually.",
                    lastMonthTotal, thisMonthTotal);
        }
        response.setTwinComparisonMessage(message);
    }

    // ------------------------------------------------------------------
    // 3) PACE ANALYSIS (Early Warning System)
    // For each category, estimate how many days are left before the budget runs out,
    // based on how fast money has been spent so far this month.
    // ------------------------------------------------------------------
    private void buildPaceAnalysis(DashboardResponse response, User user, LocalDate today) {
        List<DashboardResponse.PaceWarning> warnings = new ArrayList<>();
        YearMonth thisMonth = YearMonth.from(today);
        int dayOfMonth = today.getDayOfMonth(); // how many days have elapsed this month
        int totalDaysInMonth = thisMonth.lengthOfMonth();

        List<Category> categories = categoryRepository.findByOwner(user);

        for (Category category : categories) {
            double spentSoFar = sumTransactionsForCategoryBetween(user, category, thisMonth.atDay(1), today);
            double remaining = category.getMonthlyLimit() - spentSoFar;

            // Daily burn rate = how much money disappears per day, on average, so far this month.
            double dailyBurnRate = spentSoFar / dayOfMonth;

            int daysLeftAtCurrentPace;
            if (dailyBurnRate <= 0) {
                // No spending yet -> budget theoretically lasts the rest of the month.
                daysLeftAtCurrentPace = totalDaysInMonth - dayOfMonth;
            } else {
                daysLeftAtCurrentPace = (int) Math.floor(remaining / dailyBurnRate);
            }

            int daysRemainingInMonth = totalDaysInMonth - dayOfMonth;

            // Only warn the user if they're on track to run out BEFORE the month ends.
            if (remaining > 0 && daysLeftAtCurrentPace < daysRemainingInMonth) {
                DashboardResponse.PaceWarning warning = new DashboardResponse.PaceWarning();
                warning.categoryName = category.getName();
                warning.remainingBudget = remaining;
                warning.daysLeftAtCurrentPace = daysLeftAtCurrentPace;
                warning.message = String.format(
                        "You have $%.2f left in your '%s' budget, but at your current pace, " +
                        "you'll run out in %d day(s) - not %d. Time to slow down!",
                        remaining, category.getName(), daysLeftAtCurrentPace, daysRemainingInMonth);
                warnings.add(warning);
            } else if (remaining <= 0) {
                DashboardResponse.PaceWarning warning = new DashboardResponse.PaceWarning();
                warning.categoryName = category.getName();
                warning.remainingBudget = remaining;
                warning.daysLeftAtCurrentPace = 0;
                warning.message = String.format(
                        "Your '%s' budget is already gone (you're $%.2f over). It's not looking good, chief.",
                        category.getName(), Math.abs(remaining));
                warnings.add(warning);
            }
        }

        response.setPaceWarnings(warnings);
    }

    // ------------------------------------------------------------------
    // 4) DREAM PROTECTOR (now supports multiple dreams at once)
    // Translates this month's overspending or savings into a percentage of EACH dream.
    // The dollar amount itself isn't split between dreams - each dream independently shows
    // what that same overspend/saving would mean for IT specifically (a $200 overspend is
    // 10% of a $2000 dream, but only 4% of a $5000 dream - both are shown, side by side).
    // ------------------------------------------------------------------
    private void buildDreamProtector(DashboardResponse response, User user, LocalDate today) {
        List<Dream> dreams = dreamRepository.findByOwner(user);
        if (dreams.isEmpty()) {
            response.setDreamStatuses(List.of()); // user hasn't set a dream yet
            return;
        }

        double totalBudget = getTotalMonthlyBudget(user);
        double thisMonthTotal = response.getThisMonthTotal();
        double difference = thisMonthTotal - totalBudget; // positive = overspent, negative = saved

        List<DashboardResponse.DreamStatus> statuses = new ArrayList<>();
        for (Dream dream : dreams) {
            statuses.add(buildSingleDreamStatus(dream, difference));
        }
        response.setDreamStatuses(statuses);
    }

    private DashboardResponse.DreamStatus buildSingleDreamStatus(Dream dream, double difference) {
        DashboardResponse.DreamStatus status = new DashboardResponse.DreamStatus();
        status.dreamName = dream.getName();
        status.targetAmount = dream.getTargetAmount();

        if (difference > 0) {
            // Overspent: this "damages" the dream.
            double damagePercent = (difference / dream.getTargetAmount()) * 100.0;
            status.savedAmount = dream.getSavedAmount(); // unchanged, no progress this month
            status.progressPercent = (dream.getSavedAmount() / dream.getTargetAmount()) * 100.0;
            status.message = String.format(
                    "You overspent your budget by $%.2f this month. Let me translate that for you: " +
                    "that's %.1f%% of your '%s' dream. Hope it was worth it!",
                    difference, damagePercent, dream.getName());
        } else {
            // Saved money: this brings the dream closer!
            double savedThisMonth = Math.abs(difference);
            double newSavedAmount = dream.getSavedAmount() + savedThisMonth;
            // Note: for a full app you would persist this back to the database.
            // We keep this read-only here and just show the projection.
            double progressPercent = (newSavedAmount / dream.getTargetAmount()) * 100.0;

            status.savedAmount = newSavedAmount;
            status.progressPercent = Math.min(progressPercent, 100.0);
            status.message = String.format(
                    "Great news! You saved $%.2f this month. That means you're now %.1f%% closer to your '%s' dream. " +
                    "Your future self is already celebrating. Keep this energy!",
                    savedThisMonth, progressPercent, dream.getName());
        }
        return status;
    }

    // ------------------------------------------------------------------
    // 5) MEMORY LANE TIMELINE
    // Summarizes the last 6 months in one friendly line each.
    // ------------------------------------------------------------------
    private void buildMemoryLane(DashboardResponse response, User user, LocalDate today) {
        List<DashboardResponse.MemoryLaneEntry> entries = new ArrayList<>();
        double totalBudget = getTotalMonthlyBudget(user);

        for (int i = 1; i <= 6; i++) {
            YearMonth month = YearMonth.from(today).minusMonths(i);
            double total = sumTransactionsBetween(user, month.atDay(1), month.atEndOfMonth());

            DashboardResponse.MemoryLaneEntry entry = new DashboardResponse.MemoryLaneEntry();
            entry.monthLabel = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
            entry.totalSpent = total;

            if (total == 0) {
                entry.message = "No data recorded that month.";
            } else if (total <= totalBudget) {
                double saved = totalBudget - total;
                entry.message = String.format("You were a money-saving machine! You saved $%.2f.", saved);
            } else {
                double overspent = total - totalBudget;
                entry.message = String.format(
                        "We don't talk about this month. You overspent by $%.2f.", overspent);
            }
            entries.add(entry);
        }

        response.setMemoryLane(entries);
    }

    // ------------------------------------------------------------------
    // 6) TALKING REPORTS (weekly on Friday, monthly on the last day of the month)
    // ------------------------------------------------------------------
    private void buildPeriodicReport(DashboardResponse response, User user, LocalDate today) {
        String message = null;

        boolean isLastDayOfMonth = today.equals(YearMonth.from(today).atEndOfMonth());
        boolean isFriday = today.getDayOfWeek() == DayOfWeek.FRIDAY;

        double totalBudget = getTotalMonthlyBudget(user);
        double thisMonthTotal = response.getThisMonthTotal();
        double difference = totalBudget - thisMonthTotal; // positive = saved, negative = overspent

        if (isLastDayOfMonth) {
            if (difference >= 0) {
                message = String.format(
                        "BOOM! The month is over. You actually SAVED $%.2f this month! Who are you, Warren Buffett? Proud of you, champ.",
                        difference);
            } else {
                message = String.format(
                        "Well... that was a financial horror movie. You overspent by $%.2f this month. " +
                        "Let's pretend this month never happened. Fresh start tomorrow?",
                        Math.abs(difference));
            }
        } else if (isFriday) {
            LocalDate weekStart = today.minusDays(6);
            double weekTotal = sumTransactionsBetween(user, weekStart, today);
            double weeklyBudget = totalBudget / 4.0;
            if (weekTotal <= weeklyBudget) {
                message = String.format(
                        "It's Friday! You spent $%.2f this week, under your weekly budget of $%.2f. Nice control!",
                        weekTotal, weeklyBudget);
            } else {
                message = String.format(
                        "It's Friday! You spent $%.2f this week, over your weekly budget of $%.2f. The weekend called - it wants a break from your wallet.",
                        weekTotal, weeklyBudget);
            }
        }

        response.setPeriodicReportMessage(message);
    }

    // ------------------------------------------------------------------
    // FEATURE 2 SUMMARY: how many hours of work has this month's spending cost?
    // ------------------------------------------------------------------
    private void buildTimeCostSummary(DashboardResponse response, User user, LocalDate today) {
        Double hourlyRate = timeCostService.getHourlyRate(user);
        if (hourlyRate == null) {
            response.setTimeCostSummaryMessage(null); // user hasn't set up salary/working hours yet
            return;
        }

        double thisMonthTotal = response.getThisMonthTotal();
        Double totalHours = timeCostService.calculateTimeCostHours(thisMonthTotal, hourlyRate);
        if (totalHours == null) {
            response.setTimeCostSummaryMessage(null);
            return;
        }

        double workDays = totalHours / 8.0;
        response.setTimeCostSummaryMessage(String.format(
                "This month's spending cost you about %.1f hours of work - roughly %.1f full working day(s).",
                totalHours, workDays));
    }

    // ------------------------------------------------------------------
    // Small reusable helper methods
    // ------------------------------------------------------------------

    /** Adds up this user's transaction amounts between two dates (inclusive). */
    private double sumTransactionsBetween(User user, LocalDate start, LocalDate end) {
        List<Transaction> transactions = transactionRepository.findByOwnerAndDateBetween(user, start, end);
        double total = 0.0;
        for (Transaction t : transactions) {
            total += t.getAmount();
        }
        return total;
    }

    /** Same as above, but only for one category. */
    private double sumTransactionsForCategoryBetween(User user, Category category, LocalDate start, LocalDate end) {
        List<Transaction> transactions = transactionRepository.findByOwnerAndCategoryAndDateBetween(user, category, start, end);
        double total = 0.0;
        for (Transaction t : transactions) {
            total += t.getAmount();
        }
        return total;
    }

    /** Sums up the monthlyLimit of every category this user owns = their total monthly budget. */
    private double getTotalMonthlyBudget(User user) {
        List<Category> categories = categoryRepository.findByOwner(user);
        double total = 0.0;
        for (Category c : categories) {
            total += c.getMonthlyLimit();
        }
        return total;
    }
}
