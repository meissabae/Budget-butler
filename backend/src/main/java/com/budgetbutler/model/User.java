package com.budgetbutler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents one registered user of the app.
 * IMPORTANT: "password" here is never the raw password the user typed - it's a
 * one-way hash (produced by BCrypt). Even we, looking at the database, can't
 * see the original password. When someone logs in, we hash what they typed
 * and compare the hashes - we never "decrypt" anything.
 */
@Entity
@Table(name = "app_user") // "user" is a reserved word in some databases, so we use "app_user"
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // stores the BCrypt HASH, not the plain password

    private String name;

    // Starts false; set to true once the user clicks the link in their verification email.
    // We do NOT block login for unverified users (that's a product decision, not a technical
    // limit) - we just show a friendly reminder banner. See DashboardResponse.emailVerified.
    private boolean emailVerified = false;

    // --- Subscription / trial fields ---

    // The moment the account was created - the 20-day trial is counted from here.
    private LocalDateTime trialStartDate;

    @Enumerated(EnumType.STRING) // stores "TRIAL" / "ACTIVE" / "EXPIRED" as readable text in the DB
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    // Which PAID tier this user bought (only meaningful once subscriptionStatus = ACTIVE).
    // During TRIAL, the user gets full PREMIUM access regardless of this field.
    @Enumerated(EnumType.STRING)
    private PlanTier planTier = PlanTier.FREE;

    // Stripe's own ids for this customer/subscription - used to look the user up
    // when Stripe sends us a webhook event, and to manage their subscription later.
    private String stripeCustomerId;
    private String stripeSubscriptionId;

    public User() {
    }

    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.trialStartDate = LocalDateTime.now();
        this.subscriptionStatus = SubscriptionStatus.TRIAL;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getTrialStartDate() {
        return trialStartDate;
    }

    public void setTrialStartDate(LocalDateTime trialStartDate) {
        this.trialStartDate = trialStartDate;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public PlanTier getPlanTier() {
        return planTier;
    }

    public void setPlanTier(PlanTier planTier) {
        this.planTier = planTier;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }
}
