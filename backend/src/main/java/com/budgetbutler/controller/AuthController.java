package com.budgetbutler.controller;

import com.budgetbutler.dto.*;
import com.budgetbutler.model.User;
import com.budgetbutler.model.VerificationToken;
import com.budgetbutler.repository.UserRepository;
import com.budgetbutler.repository.VerificationTokenRepository;
import com.budgetbutler.security.JwtService;
import com.budgetbutler.security.RateLimiter;
import com.budgetbutler.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The only endpoints that DON'T require a token (see SecurityConfig: "/api/auth/**" is public).
 * Everything else in the app requires a valid JWT obtained from login/register.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RateLimiter rateLimiter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String rateLimitKey = "register:" + clientIp(httpRequest);
        if (rateLimiter.isBlocked(rateLimitKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many registration attempts. Please try again in a few minutes.");
        }

        if (userRepository.existsByEmail(request.email())) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            return ResponseEntity.badRequest().body("An account with this email already exists.");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), hashedPassword, request.name());
        userRepository.save(user);
        rateLimiter.reset(rateLimitKey);

        sendVerificationEmail(user);

        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // Rate limit by IP + email together, so one attacker can't lock out a real user's
        // account just by spamming failed attempts against their email from a different IP.
        String rateLimitKey = "login:" + clientIp(httpRequest) + ":" + request.email();
        if (rateLimiter.isBlocked(rateLimitKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many failed login attempts. Please wait 15 minutes and try again.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (Exception e) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            return ResponseEntity.status(401).body("Invalid email or password.");
        }

        rateLimiter.reset(rateLimitKey);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getName()));
    }

    /**
     * Always responds the same way whether or not the email exists - this prevents someone
     * from using this endpoint to check which emails have accounts ("user enumeration").
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            VerificationToken resetToken = new VerificationToken(
                    token, VerificationToken.TokenPurpose.PASSWORD_RESET,
                    LocalDateTime.now().plusHours(1), user);
            verificationTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });

        return ResponseEntity.ok("If an account with that email exists, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        VerificationToken token = verificationTokenRepository.findByToken(request.token())
                .orElse(null);

        if (token == null || token.getPurpose() != VerificationToken.TokenPurpose.PASSWORD_RESET || token.isExpired()) {
            return ResponseEntity.badRequest().body("This reset link is invalid or has expired.");
        }

        User user = token.getOwner();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        verificationTokenRepository.delete(token); // single-use - can't be replayed

        return ResponseEntity.ok("Password updated. You can now log in with your new password.");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token).orElse(null);

        if (verificationToken == null || verificationToken.getPurpose() != VerificationToken.TokenPurpose.EMAIL_VERIFICATION
                || verificationToken.isExpired()) {
            return ResponseEntity.badRequest().body("This verification link is invalid or has expired.");
        }

        User user = verificationToken.getOwner();
        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);

        return ResponseEntity.ok("Email verified! You can close this tab.");
    }

    /** Public endpoint (like the others under /api/auth/**) - takes an email, not a token,
     * so a logged-out user who never got their first verification email can still request one. */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user != null && !user.isEmailVerified()) {
            sendVerificationEmail(user);
        }
        return ResponseEntity.ok("If your account needs verification, a new email has been sent.");
    }

    private void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(
                token, VerificationToken.TokenPurpose.EMAIL_VERIFICATION,
                LocalDateTime.now().plusHours(24), user);
        verificationTokenRepository.save(verificationToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), verificationLink);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
