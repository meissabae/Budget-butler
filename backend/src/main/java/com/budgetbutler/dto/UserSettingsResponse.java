package com.budgetbutler.dto;

// What GET /api/settings returns. "configured" tells the frontend whether to show
// the onboarding form or the normal dashboard.
public record UserSettingsResponse(
        boolean configured,
        Double monthlySalary,
        Integer salaryPaymentDay,
        Double monthlyWorkingHours,
        String currency,
        Long salaryWalletId
) {
}
