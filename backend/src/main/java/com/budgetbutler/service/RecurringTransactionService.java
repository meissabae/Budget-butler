package com.budgetbutler.service;

import com.budgetbutler.model.RecurringTransaction;
import com.budgetbutler.model.Transaction;
import com.budgetbutler.model.User;
import com.budgetbutler.repository.RecurringTransactionRepository;
import com.budgetbutler.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Same pattern as WalletService's automatic salary deposit: checked every time the dashboard
 * loads, generates a real Transaction for each active recurring rule whose day has arrived
 * this month and hasn't already been generated - so rent and subscriptions log themselves
 * without the user re-typing them every single month.
 */
@Service
public class RecurringTransactionService {

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    public void generateDueTransactions(User user) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        String thisMonthKey = thisMonth.toString(); // e.g. "2026-07"

        for (RecurringTransaction rule : recurringTransactionRepository.findByOwnerAndActiveTrue(user)) {
            if (rule.getCategory() == null || rule.getDayOfMonth() == null) continue;

            boolean alreadyGeneratedThisMonth = thisMonthKey.equals(rule.getLastGeneratedMonth());
            if (alreadyGeneratedThisMonth) continue;

            int safeDay = Math.min(rule.getDayOfMonth(), thisMonth.lengthOfMonth());
            LocalDate dueDate = thisMonth.atDay(safeDay);
            if (today.isBefore(dueDate)) continue; // not due yet this month

            Transaction transaction = new Transaction(
                    rule.getDescription(),
                    rule.getAmount(),
                    dueDate,
                    rule.getCategory(),
                    user
            );
            transactionRepository.save(transaction);
            walletService.debit(rule.getCategory().getWallet(), rule.getAmount());

            rule.setLastGeneratedMonth(thisMonthKey);
            recurringTransactionRepository.save(rule);
        }
    }
}
