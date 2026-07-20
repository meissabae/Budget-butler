package com.budgetbutler.controller;

import com.budgetbutler.dto.CheckoutRequest;
import com.budgetbutler.dto.CheckoutSessionResponse;
import com.budgetbutler.dto.SubscriptionStatusResponse;
import com.budgetbutler.model.PlanTier;
import com.budgetbutler.model.SubscriptionStatus;
import com.budgetbutler.model.User;
import com.budgetbutler.repository.UserRepository;
import com.budgetbutler.security.CurrentUserProvider;
import com.budgetbutler.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    // 4 separate Stripe Prices - one per (tier x billing interval) combination.
    @Value("${stripe.price-id-plus-monthly}")
    private String priceIdPlusMonthly;

    @Value("${stripe.price-id-plus-annual}")
    private String priceIdPlusAnnual;

    @Value("${stripe.price-id-premium-monthly}")
    private String priceIdPremiumMonthly;

    @Value("${stripe.price-id-premium-annual}")
    private String priceIdPremiumAnnual;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /** Tells the frontend: which billing state and effective feature tier is the user in right now. */
    @GetMapping("/status")
    public SubscriptionStatusResponse getStatus() {
        User user = currentUserProvider.getCurrentUser();
        return new SubscriptionStatusResponse(
                user.getSubscriptionStatus().name(),
                subscriptionService.getEffectiveTier(user).name(),
                subscriptionService.getTrialDaysRemaining(user)
        );
    }

    /**
     * Creates a Stripe "Checkout Session" for the chosen tier + billing interval.
     * We just hand back the URL; the Angular app redirects the browser there.
     * We never see or touch the user's card details ourselves - Stripe handles that entirely.
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CheckoutRequest request) {
        User user = currentUserProvider.getCurrentUser();

        String priceId = resolvePriceId(request.tier(), request.interval());
        if (priceId == null) {
            return ResponseEntity.badRequest().body("Invalid tier or interval.");
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(user.getEmail())
                    // client_reference_id lets us match the payment back to OUR user.
                    .setClientReferenceId(user.getId().toString())
                    // metadata travels with the session so the webhook knows WHICH tier was bought -
                    // Stripe doesn't otherwise tell us "this price = PLUS" on its own.
                    .putMetadata("tier", request.tier())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setSuccessUrl(frontendUrl + "/billing?success=true")
                    .setCancelUrl(frontendUrl + "/billing?canceled=true")
                    .build();

            Session session = Session.create(params);
            return ResponseEntity.ok(new CheckoutSessionResponse(session.getUrl()));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body("Could not start checkout: " + e.getMessage());
        }
    }

    private String resolvePriceId(String tier, String interval) {
        if (tier == null || interval == null) return null;
        Map<String, String> priceMap = Map.of(
                "PLUS_MONTHLY", priceIdPlusMonthly,
                "PLUS_ANNUAL", priceIdPlusAnnual,
                "PREMIUM_MONTHLY", priceIdPremiumMonthly,
                "PREMIUM_ANNUAL", priceIdPremiumAnnual
        );
        return priceMap.get(tier.toUpperCase() + "_" + interval.toUpperCase());
    }

    /**
     * Stripe calls THIS endpoint automatically (not the browser) once a payment succeeds.
     * This is why it must stay PUBLIC in SecurityConfig - Stripe doesn't have a JWT token,
     * it proves it's really Stripe via a cryptographic signature instead (checked below).
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                 @RequestHeader("Stripe-Signature") String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                activateSubscription(session);
            }
        } else if ("customer.subscription.deleted".equals(event.getType())) {
            // Extension point: look up the user by stripeSubscriptionId and reset them
            // to subscriptionStatus = EXPIRED so they drop back to the FREE tier.
        }

        return ResponseEntity.ok("received");
    }

    private void activateSubscription(Session session) {
        String userId = session.getClientReferenceId();
        if (userId == null) return;

        String tierName = session.getMetadata() != null ? session.getMetadata().get("tier") : null;
        PlanTier tier = "PLUS".equalsIgnoreCase(tierName) ? PlanTier.PLUS : PlanTier.PREMIUM;

        Optional<User> userOpt = userRepository.findById(Long.parseLong(userId));
        userOpt.ifPresent(user -> {
            user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            user.setPlanTier(tier);
            user.setStripeCustomerId(session.getCustomer());
            user.setStripeSubscriptionId(session.getSubscription());
            userRepository.save(user);
        });
    }
}
