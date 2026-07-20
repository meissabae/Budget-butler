package com.budgetbutler.dto;

// What Angular sends to PUT /api/settings
public record UserSettingsRequest(
        Double monthlySalary,
        Integer salaryPaymentDay,
        Double monthlyWorkingHours,
        String currency,
        Long salaryWalletId
) {
}
