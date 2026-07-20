package com.budgetbutler.controller;

import com.budgetbutler.dto.RecurringTransactionRequest;
import com.budgetbutler.dto.RecurringTransactionResponse;
import com.budgetbutler.model.Category;
import com.budgetbutler.model.RecurringTransaction;
import com.budgetbutler.model.User;
import com.budgetbutler.repository.CategoryRepository;
import com.budgetbutler.repository.RecurringTransactionRepository;
import com.budgetbutler.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<RecurringTransactionResponse> getAll() {
        User user = currentUserProvider.getCurrentUser();
        return recurringTransactionRepository.findByOwner(user).stream()
                .map(RecurringTransactionResponse::from).toList();
    }

    @PostMapping
    public RecurringTransactionResponse create(@RequestBody RecurringTransactionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Category category = resolveOwnedCategory(request.categoryId(), user);

        RecurringTransaction rule = new RecurringTransaction();
        rule.setDescription(request.description());
        rule.setAmount(request.amount());
        rule.setDayOfMonth(request.dayOfMonth());
        rule.setCategory(category);
        rule.setOwner(user);
        rule.setActive(request.active() == null || request.active());

        return RecurringTransactionResponse.from(recurringTransactionRepository.save(rule));
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(@PathVariable Long id, @RequestBody RecurringTransactionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        RecurringTransaction rule = findOwnedRule(id, user);
        Category category = resolveOwnedCategory(request.categoryId(), user);

        rule.setDescription(request.description());
        rule.setAmount(request.amount());
        rule.setDayOfMonth(request.dayOfMonth());
        rule.setCategory(category);
        if (request.active() != null) {
            rule.setActive(request.active());
        }

        return RecurringTransactionResponse.from(recurringTransactionRepository.save(rule));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        RecurringTransaction rule = findOwnedRule(id, user);
        recurringTransactionRepository.deleteById(rule.getId());
    }

    private RecurringTransaction findOwnedRule(Long id, User user) {
        RecurringTransaction rule = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
        if (!rule.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to modify this recurring transaction.");
        }
        return rule;
    }

    private Category resolveOwnedCategory(Long categoryId, User user) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (!category.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("This category doesn't belong to you.");
        }
        return category;
    }
}
