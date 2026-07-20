package com.budgetbutler.security;

import com.budgetbutler.model.User;
import com.budgetbutler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * By the time a request reaches a controller, JwtAuthFilter has already validated the token
 * and told Spring "this request belongs to user with email X" (stored in the SecurityContext).
 * This class is a small shortcut so every controller can ask "who is making this request?"
 * without repeating the same 3 lines everywhere.
 */
@Component
public class CurrentUserProvider {

    @Autowired
    private UserRepository userRepository;

    public User getCurrentUser() {
        // getAuthentication().getName() returns the email we set as the "subject" of the JWT.
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}
