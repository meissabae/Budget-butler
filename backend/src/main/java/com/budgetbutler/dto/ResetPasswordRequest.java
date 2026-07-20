package com.budgetbutler.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
