package com.budgetbutler.dto;

// What we send BACK to Angular after a successful register/login: the token to store,
// plus a couple of display details so the frontend doesn't need a separate call to show them.
public record AuthResponse(String token, String email, String name) {
}
