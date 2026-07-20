package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * A template for an expense that happens every month - rent, a Netflix subscription, etc.
 * Unlike a regular Transaction (one specific expense on one specific date), this is a
 * recurring RULE: "charge $15 to 'Subscriptions' on the 5th of every month".
 * RecurringTransactionService turns this rule into a real Transaction automatically,
 * the same way WalletService turns the salary rule into a real wallet deposit.
 */
@Entity
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description; // e.g. "Netflix subscription"

    private Double amount;

    private Integer dayOfMonth; // 1-31, clamped to the actual month length when generating

    private boolean active = true; // lets the user pause a recurring expense without deleting it

    // Tracks "YYYY-MM" of the last month this rule already generated a transaction for,
    // so we don't accidentally create duplicates if the dashboard loads twice in one day.
    private String lastGeneratedMonth;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User owner;

    public RecurringTransaction() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getLastGeneratedMonth() {
        return lastGeneratedMonth;
    }

    public void setLastGeneratedMonth(String lastGeneratedMonth) {
        this.lastGeneratedMonth = lastGeneratedMonth;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
