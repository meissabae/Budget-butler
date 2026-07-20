package com.budgetbutler.dto;

// The JSON shape Angular sends to POST /api/auth/login
public record LoginRequest(String email, String password) {
}
