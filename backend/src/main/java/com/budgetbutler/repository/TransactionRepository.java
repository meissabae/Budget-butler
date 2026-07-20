package com.budgetbutler.repository;

import com.budgetbutler.model.Category;
import com.budgetbutler.model.Transaction;
import com.budgetbutler.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All transactions belonging to one user (used internally by other services for totals -
    // NOT used by the main list endpoint anymore, which is paginated below).
    List<Transaction> findByOwner(User owner);

    // Paginated + sorted newest-first version, used by the Transactions page in the UI.
    // Spring Data automatically implements this from the method name - "OrderByDateDesc"
    // becomes "ORDER BY date DESC" in the generated SQL.
    Page<Transaction> findByOwnerOrderByDateDesc(User owner, Pageable pageable);

    // Same idea, but also restricted to a date range - used for "this month vs last month" etc.
    // "findByOwnerAndDateBetween" -> WHERE owner = ?1 AND date BETWEEN ?2 AND ?3
    List<Transaction> findByOwnerAndDateBetween(User owner, LocalDate start, LocalDate end);

    // Same idea, but also filtered by category. Useful for the Pace Analysis feature.
    List<Transaction> findByOwnerAndCategoryAndDateBetween(User owner, Category category, LocalDate start, LocalDate end);
}
