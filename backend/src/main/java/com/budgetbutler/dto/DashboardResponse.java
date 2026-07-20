package com.budgetbutler.dto;

import java.util.List;

/**
 * This is the "shape" of the JSON object we send to the Angular frontend
 * every time it loads the dashboard. Think of it as one big envelope
 * containing all the emotional features described in the app pitch.
 *
 * Using a plain class (not a record) here because it has many fields -
 * a simple constructor + getters is easier to read for a beginner.
 */
public class DashboardResponse {

    private String nickname;          // e.g. "The Wise Owl"
    private String dailyTitleMessage;  // e.g. "You only spent $20 yesterday..."

    private Double thisMonthTotal;
    private Double lastMonthTotal;
    private String twinComparisonMessage;

    // --- Wallets & Currency (always available, not gated by subscription tier) ---
    private List<WalletResponse> wallets;
    private String currency = "USD";
    private boolean emailVerified;

    private List<PaceWarning> paceWarnings;

    private List<DreamStatus> dreamStatuses; // empty list if the user has no dreams yet

    private List<MemoryLaneEntry> memoryLane;

    private String periodicReportMessage; // null unless today is Friday or month-end

    // --- Subscription / paywall fields ---
    private String currentTier;         // "FREE", "PLUS", or "PREMIUM" - the effective tier right now
    private boolean plusLocked;         // true = FREE tier -> Daily Title/Twin message/Pace Analysis hidden
    private boolean premiumLocked;      // true = FREE or PLUS tier -> Dream Protector/Memory Lane/Reports/Payday/TimeCost/Garden hidden
    private int trialDaysRemaining;     // shown as a countdown badge while still on trial
    private String upgradeMessage;      // shown when plusLocked = true (upsell to Plus)
    private String premiumUpsellMessage; // shown when premiumLocked = true but plusLocked = false (upsell to Premium)

    // --- Feature 1: Salary Awareness ---
    private List<String> salaryInsights; // empty if the user hasn't set up salary/payday yet

    // --- Feature 2: Time Cost of Purchases ---
    private String timeCostSummaryMessage; // e.g. "This month's spending cost you about 38 hours of work."

    // --- Feature 3: Dream Garden ---
    private GardenResponse gardenStatus;

    // --- Streaks & Badges ---
    private StreakResponse streakStatus;

    public DashboardResponse() {
    }

    // --- Getters and setters ---

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDailyTitleMessage() {
        return dailyTitleMessage;
    }

    public void setDailyTitleMessage(String dailyTitleMessage) {
        this.dailyTitleMessage = dailyTitleMessage;
    }

    public Double getThisMonthTotal() {
        return thisMonthTotal;
    }

    public void setThisMonthTotal(Double thisMonthTotal) {
        this.thisMonthTotal = thisMonthTotal;
    }

    public Double getLastMonthTotal() {
        return lastMonthTotal;
    }

    public void setLastMonthTotal(Double lastMonthTotal) {
        this.lastMonthTotal = lastMonthTotal;
    }

    public String getTwinComparisonMessage() {
        return twinComparisonMessage;
    }

    public void setTwinComparisonMessage(String twinComparisonMessage) {
        this.twinComparisonMessage = twinComparisonMessage;
    }

    public List<WalletResponse> getWallets() {
        return wallets;
    }

    public void setWallets(List<WalletResponse> wallets) {
        this.wallets = wallets;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public List<PaceWarning> getPaceWarnings() {
        return paceWarnings;
    }

    public void setPaceWarnings(List<PaceWarning> paceWarnings) {
        this.paceWarnings = paceWarnings;
    }

    public List<DreamStatus> getDreamStatuses() {
        return dreamStatuses;
    }

    public void setDreamStatuses(List<DreamStatus> dreamStatuses) {
        this.dreamStatuses = dreamStatuses;
    }

    public List<MemoryLaneEntry> getMemoryLane() {
        return memoryLane;
    }

    public void setMemoryLane(List<MemoryLaneEntry> memoryLane) {
        this.memoryLane = memoryLane;
    }

    public String getPeriodicReportMessage() {
        return periodicReportMessage;
    }

    public void setPeriodicReportMessage(String periodicReportMessage) {
        this.periodicReportMessage = periodicReportMessage;
    }

    public String getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(String currentTier) {
        this.currentTier = currentTier;
    }

    public boolean isPlusLocked() {
        return plusLocked;
    }

    public void setPlusLocked(boolean plusLocked) {
        this.plusLocked = plusLocked;
    }

    public boolean isPremiumLocked() {
        return premiumLocked;
    }

    public void setPremiumLocked(boolean premiumLocked) {
        this.premiumLocked = premiumLocked;
    }

    public int getTrialDaysRemaining() {
        return trialDaysRemaining;
    }

    public void setTrialDaysRemaining(int trialDaysRemaining) {
        this.trialDaysRemaining = trialDaysRemaining;
    }

    public String getUpgradeMessage() {
        return upgradeMessage;
    }

    public void setUpgradeMessage(String upgradeMessage) {
        this.upgradeMessage = upgradeMessage;
    }

    public String getPremiumUpsellMessage() {
        return premiumUpsellMessage;
    }

    public void setPremiumUpsellMessage(String premiumUpsellMessage) {
        this.premiumUpsellMessage = premiumUpsellMessage;
    }

    public List<String> getSalaryInsights() {
        return salaryInsights;
    }

    public void setSalaryInsights(List<String> salaryInsights) {
        this.salaryInsights = salaryInsights;
    }

    public String getTimeCostSummaryMessage() {
        return timeCostSummaryMessage;
    }

    public void setTimeCostSummaryMessage(String timeCostSummaryMessage) {
        this.timeCostSummaryMessage = timeCostSummaryMessage;
    }

    public GardenResponse getGardenStatus() {
        return gardenStatus;
    }

    public void setGardenStatus(GardenResponse gardenStatus) {
        this.gardenStatus = gardenStatus;
    }

    public StreakResponse getStreakStatus() {
        return streakStatus;
    }

    public void setStreakStatus(StreakResponse streakStatus) {
        this.streakStatus = streakStatus;
    }

    // --- Small nested "sub objects" used inside the dashboard ---

    /** One category's burn-rate warning, e.g. "Marketing will run out in 2 days". */
    public static class PaceWarning {
        public String categoryName;
        public Double remainingBudget;
        public int daysLeftAtCurrentPace;
        public String message;
    }

    /** The Dream Protector section. */
    public static class DreamStatus {
        public String dreamName;
        public Double targetAmount;
        public Double savedAmount;
        public Double progressPercent;
        public String message;
    }

    /** One row in the "Memory Lane" timeline, e.g. "January: saved $400". */
    public static class MemoryLaneEntry {
        public String monthLabel; // e.g. "January 2026"
        public Double totalSpent;
        public String message;
    }
}
