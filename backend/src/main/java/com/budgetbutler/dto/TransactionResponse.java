package com.budgetbutler.dto;

import com.budgetbutler.model.Transaction;

import java.time.LocalDate;

/**
 * We don't store "time cost" in the database - it depends on the user's CURRENT salary
 * settings, which can change. So instead we compute it fresh every time and wrap it
 * around the Transaction data in this response shape.
 */
public class TransactionResponse {

    private Long id;
    private String description;
    private Double amount;
    private LocalDate date;
    private String categoryName;
    private Long categoryId;

    private Double timeCostHours; // null if the user hasn't set up salary/hours yet
    private String timeCostMessage; // e.g. "This cost you 2.0 hour(s) of work."

    public static TransactionResponse from(Transaction t, Double timeCostHours, String timeCostMessage) {
        TransactionResponse response = new TransactionResponse();
        response.id = t.getId();
        response.description = t.getDescription();
        response.amount = t.getAmount();
        response.date = t.getDate();
        response.categoryName = t.getCategory() != null ? t.getCategory().getName() : null;
        response.categoryId = t.getCategory() != null ? t.getCategory().getId() : null;
        response.timeCostHours = timeCostHours;
        response.timeCostMessage = timeCostMessage;
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Double getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Double getTimeCostHours() {
        return timeCostHours;
    }

    public String getTimeCostMessage() {
        return timeCostMessage;
    }
}
