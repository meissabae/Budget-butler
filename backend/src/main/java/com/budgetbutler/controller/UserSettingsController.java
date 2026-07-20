package com.budgetbutler.controller;

import com.budgetbutler.dto.UserSettingsRequest;
import com.budgetbutler.dto.UserSettingsResponse;
import com.budgetbutler.model.User;
import com.budgetbutler.model.UserSettings;
import com.budgetbutler.model.Wallet;
import com.budgetbutler.repository.UserSettingsRepository;
import com.budgetbutler.repository.WalletRepository;
import com.budgetbutler.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @GetMapping
    public UserSettingsResponse getSettings() {
        User user = currentUserProvider.getCurrentUser();
        return userSettingsRepository.findByOwner(user)
                .map(this::toResponse)
                .orElse(new UserSettingsResponse(false, null, null, null, "USD", null));
    }

    // One endpoint handles both "first time setup" and "editing later" -
    // we just check whether a row already exists for this user.
    @PutMapping
    public UserSettingsResponse saveSettings(@RequestBody UserSettingsRequest request) {
        User user = currentUserProvider.getCurrentUser();

        UserSettings settings = userSettingsRepository.findByOwner(user)
                .orElse(new UserSettings());

        settings.setOwner(user);
        settings.setMonthlySalary(request.monthlySalary());
        settings.setSalaryPaymentDay(request.salaryPaymentDay());
        settings.setMonthlyWorkingHours(request.monthlyWorkingHours());
        settings.setCurrency(request.currency() != null ? request.currency() : "USD");

        if (request.salaryWalletId() != null) {
            Wallet wallet = walletRepository.findById(request.salaryWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            if (!wallet.getOwner().getId().equals(user.getId())) {
                throw new RuntimeException("This wallet doesn't belong to you.");
            }
            settings.setSalaryWallet(wallet);
        } else {
            settings.setSalaryWallet(null);
        }

        userSettingsRepository.save(settings);

        return toResponse(settings);
    }

    private UserSettingsResponse toResponse(UserSettings s) {
        Long walletId = s.getSalaryWallet() != null ? s.getSalaryWallet().getId() : null;
        return new UserSettingsResponse(true, s.getMonthlySalary(), s.getSalaryPaymentDay(),
                s.getMonthlyWorkingHours(), s.getCurrency(), walletId);
    }
}
