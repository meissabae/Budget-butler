package com.budgetbutler.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The Stripe Java library needs one line of setup before we can call any Stripe API:
 * telling it our SECRET key. @PostConstruct means "run this once, right after Spring
 * finishes creating this object" - the perfect place for one-time setup like this.
 */
@Component
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}
