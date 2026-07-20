package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A single-use, time-limited token used for two purposes: "prove you own this email"
 * (email verification) and "prove you asked for a password reset" (forgot password).
 * Both flows work the same way: generate a random token, email a link containing it,
 * and when the link is clicked, look the token up here to confirm it's valid and not expired.
 */
@Entity
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    private TokenPurpose purpose;

    private LocalDateTime expiresAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User owner;

    public VerificationToken() {
    }

    public VerificationToken(String token, TokenPurpose purpose, LocalDateTime expiresAt, User owner) {
        this.token = token;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.owner = owner;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TokenPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(TokenPurpose purpose) {
        this.purpose = purpose;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public enum TokenPurpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }
}
