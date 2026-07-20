package com.budgetbutler.service;

import com.budgetbutler.dto.WalletResponse;
import com.budgetbutler.model.User;
import com.budgetbutler.model.UserSettings;
import com.budgetbutler.model.Wallet;
import com.budgetbutler.repository.UserSettingsRepository;
import com.budgetbutler.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Wallets behave like a simple ledger: every expense subtracts from the wallet linked to
 * its category (see TransactionController), and the salary automatically adds back into
 * whichever wallet the user chose in Settings, once per month, on/after their payday.
 */
@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    /** Subtracts an expense amount from a wallet's balance. Called when a transaction is created. */
    public void debit(Wallet wallet, double amount) {
        if (wallet == null) return;
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);
    }

    /** Adds money back to a wallet's balance. Called when a transaction is deleted (the expense is undone). */
    public void credit(Wallet wallet, double amount) {
        if (wallet == null) return;
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
    }

    /**
     * Checked every time the dashboard loads: if today is on/after the user's payday for
     * this month, a salary wallet is configured, AND we haven't already credited this month -
     * add the salary into that wallet automatically. This is what makes "the wallet fills up
     * when the salary comes in" happen without the user doing anything.
     */
    public void checkAndCreditSalaryIfDue(User user) {
        Optional<UserSettings> settingsOpt = userSettingsRepository.findByOwner(user);
        if (settingsOpt.isEmpty()) return;

        UserSettings settings = settingsOpt.get();
        if (settings.getSalaryWallet() == null || settings.getMonthlySalary() == null
                || settings.getSalaryPaymentDay() == null) {
            return; // salary auto-deposit isn't fully configured yet
        }

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        int safeDay = Math.min(settings.getSalaryPaymentDay(), thisMonth.lengthOfMonth());
        LocalDate payday = thisMonth.atDay(safeDay);

        String thisMonthKey = thisMonth.toString(); // e.g. "2026-07"
        boolean alreadyCreditedThisMonth = thisMonthKey.equals(settings.getLastSalaryCreditedMonth());

        if (!today.isBefore(payday) && !alreadyCreditedThisMonth) {
            credit(settings.getSalaryWallet(), settings.getMonthlySalary());
            settings.setLastSalaryCreditedMonth(thisMonthKey);
            userSettingsRepository.save(settings);
        }
    }

    /** The manual "I got paid" button - same effect, but the user triggers it instead of waiting. */
    public WalletResponse creditSalaryNow(User user) {
        UserSettings settings = userSettingsRepository.findByOwner(user)
                .orElseThrow(() -> new RuntimeException("Please fill in your salary settings first."));

        if (settings.getSalaryWallet() == null || settings.getMonthlySalary() == null) {
            throw new RuntimeException("Please choose a salary wallet and amount in Settings first.");
        }

        credit(settings.getSalaryWallet(), settings.getMonthlySalary());
        settings.setLastSalaryCreditedMonth(YearMonth.now().toString());
        userSettingsRepository.save(settings);

        return WalletResponse.from(settings.getSalaryWallet());
    }
}
