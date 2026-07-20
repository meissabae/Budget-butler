package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * A Dream is the user's personal savings goal, e.g. "MacBook Pro - $2000".
 * We keep track of the target amount and how much has been "saved" toward it so far.
 *
 * To keep things simple, this MVP supports ONE active dream at a time.
 * (You could easily extend this later to support a list of dreams with an "active" flag.)
 */
@Entity
public class Dream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g. "MacBook Pro"

    private Double targetAmount; // e.g. 2000.0

    private Double savedAmount = 0.0; // how much progress has been made so far

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User owner;

    public Dream() {
    }

    public Dream(String name, Double targetAmount) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = 0.0;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(Double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public Double getSavedAmount() {
        return savedAmount;
    }

    public void setSavedAmount(Double savedAmount) {
        this.savedAmount = savedAmount;
    }
}
