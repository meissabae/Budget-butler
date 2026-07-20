package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One garden per user. We store the LAST computed state here (instead of recalculating
 * from scratch with no memory) so we can compare "is the garden growing or shrinking
 * compared to last time?" - that comparison is what lets us say things like
 * "some leaves are falling" instead of just reporting a flat percentage.
 */
@Entity
public class Garden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnore
    private User owner;

    private int level = 1;                  // 1 (Seed) through 5 (Beautiful Garden)
    private double growthPercentage = 0.0;   // 0-100, drives the level
    private String statusMessage = "Your garden is just a seed. Every dollar you save helps it grow!";
    private LocalDateTime lastUpdated;

    public Garden() {
    }

    public Garden(User owner) {
        this.owner = owner;
        this.lastUpdated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getGrowthPercentage() {
        return growthPercentage;
    }

    public void setGrowthPercentage(double growthPercentage) {
        this.growthPercentage = growthPercentage;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
