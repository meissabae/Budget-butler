package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * The CURRENT streak is always recalculated live (same as most features in this app),
 * but the LONGEST streak needs to be remembered somewhere - otherwise, the moment a
 * streak breaks and resets to 0, the user's personal best would be lost forever.
 * That's the only thing this table stores.
 */
@Entity
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnore
    private User owner;

    private int longestStreak = 0;
    private LocalDateTime updatedAt;

    public UserStreak() {
    }

    public UserStreak(User owner) {
        this.owner = owner;
        this.updatedAt = LocalDateTime.now();
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

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
