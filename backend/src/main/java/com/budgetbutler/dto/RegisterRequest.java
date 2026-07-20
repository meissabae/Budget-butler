package com.budgetbutler.dto;

// The JSON shape Angular sends to POST /api/auth/register
public record RegisterRequest(String name, String email, String password) {
}
