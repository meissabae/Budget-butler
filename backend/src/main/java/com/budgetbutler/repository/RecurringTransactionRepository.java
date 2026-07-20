package com.budgetbutler.repository;

import com.budgetbutler.model.RecurringTransaction;
import com.budgetbutler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByOwner(User owner);

    List<RecurringTransaction> findByOwnerAndActiveTrue(User owner);
}
