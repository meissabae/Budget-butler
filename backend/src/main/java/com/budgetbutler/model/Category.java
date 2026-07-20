package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * A Category is something you budget for, e.g. "Marketing", "Restaurants", "Transportation".
 * Each category has a monthly limit (how much you PLAN to spend on it).
 *
 * @Entity tells Spring/Hibernate: "this class = a table in the database".
 * Hibernate will create a table called "category" automatically for us.
 */
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL auto-increments the id for us
    private Long id;

    private String name;

    // How much money the user plans to spend on this category per month.
    private Double monthlyLimit;

    // Which wallet this category draws from - every expense in this category also
    // debits this wallet's balance (see TransactionController).
    @ManyToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // Which user this category belongs to. @ManyToOne = many categories can belong to one user.
    // This is the key change that makes the app multi-user: every query will now filter by owner.
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User owner;

    // Hibernate requires an empty ("no-args") constructor. It's boilerplate, but necessary.
    public Category() {
    }

    public Category(String name, Double monthlyLimit) {
        this.name = name;
        this.monthlyLimit = monthlyLimit;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    // Getters and setters: how other classes read/write these private fields.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(Double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }
}
