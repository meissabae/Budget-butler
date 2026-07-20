package com.budgetbutler.dto;

// What POST /api/subscription/create-checkout-session returns.
// The frontend just redirects the browser to this URL - Stripe hosts the whole payment page.
public record CheckoutSessionResponse(String checkoutUrl) {
}
