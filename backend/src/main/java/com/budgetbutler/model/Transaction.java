package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * A Transaction is a single expense: "I spent $45 on Restaurants on July 10th".
 * Every transaction belongs to exactly one Category.
 */
@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description; // e.g. "Lunch with client"

    private Double amount; // e.g. 45.0

    private LocalDate date; // the day the money was spent

    // @ManyToOne = "many transactions can belong to one category".
    // This creates a foreign key column (category_id) in the transaction table.
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // We also store the owner directly on the transaction (not just via category).
    // This keeps our database queries simple: "give me all transactions WHERE owner = X"
    // instead of having to join through category every time.
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User owner;

    public Transaction() {
    }

    public Transaction(String description, Double amount, LocalDate date, Category category, User owner) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.owner = owner;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
