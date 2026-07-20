package com.budgetbutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * One row per user, holding the info needed for the salary-awareness and
 * time-cost features. This is a separate table from User (instead of adding
 * these columns directly onto User) to keep the "who are you" data (User)
 * separate from "your financial preferences" data (UserSettings) - a common
 * pattern as apps grow.
 *
 * @OneToOne means each user has AT MOST one settings row.
 */
@Entity
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnore
    private User owner;

    // e.g. 3000.0 (used by both Feature 1 and Feature 2)
    private Double monthlySalary;

    // Day of the month the user gets paid, 1-31 (used by Feature 1)
    private Integer salaryPaymentDay;

    // e.g. 160 (a typical full-time month) - used by Feature 2 to compute hourly rate
    private Double monthlyWorkingHours;

    // The user's chosen currency code (e.g. "USD", "EUR", "SAR", "DZD"). Applied globally -
    // every amount in the app is assumed to be in this single currency (see README for why
    // wallets don't each have their own currency).
    private String currency = "USD";

    // Which wallet the monthly salary deposits into (see WalletService.checkAndCreditSalaryIfDue).
    @ManyToOne
    @JoinColumn(name = "salary_wallet_id")
    private Wallet salaryWallet;

    // Tracks "YYYY-MM" of the last month we already credited the salary for, so we don't
    // accidentally credit it twice in the same month.
    private String lastSalaryCreditedMonth;

    public UserSettings() {
    }

    public UserSettings(User owner, Double monthlySalary, Integer salaryPaymentDay, Double monthlyWorkingHours) {
        this.owner = owner;
        this.monthlySalary = monthlySalary;
        this.salaryPaymentDay = salaryPaymentDay;
        this.monthlyWorkingHours = monthlyWorkingHours;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Double getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(Double monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public Integer getSalaryPaymentDay() {
        return salaryPaymentDay;
    }

    public void setSalaryPaymentDay(Integer salaryPaymentDay) {
        this.salaryPaymentDay = salaryPaymentDay;
    }

    public Double getMonthlyWorkingHours() {
        return monthlyWorkingHours;
    }

    public void setMonthlyWorkingHours(Double monthlyWorkingHours) {
        this.monthlyWorkingHours = monthlyWorkingHours;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Wallet getSalaryWallet() {
        return salaryWallet;
    }

    public void setSalaryWallet(Wallet salaryWallet) {
        this.salaryWallet = salaryWallet;
    }

    public String getLastSalaryCreditedMonth() {
        return lastSalaryCreditedMonth;
    }

    public void setLastSalaryCreditedMonth(String lastSalaryCreditedMonth) {
        this.lastSalaryCreditedMonth = lastSalaryCreditedMonth;
    }
}
