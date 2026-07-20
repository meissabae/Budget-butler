package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * A Wallet is a real-world "pot of money" - a bank account, cash on hand, savings, etc.
 * Categories belong to a wallet; every expense in that category also debits the wallet's
 * balance, and the user's salary can be set to refill a specific wallet automatically.
 */
@Entity
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g. "Bank Account", "Cash"

    private Double balance = 0.0;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore // never serialize the full User (and its password hash!) back to the frontend
    private User owner;

    public Wallet() {
    }

    public Wallet(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.balance = 0.0;
    }

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

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
