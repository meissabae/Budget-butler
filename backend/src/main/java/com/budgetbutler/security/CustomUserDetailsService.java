package com.budgetbutler.security;

import com.budgetbutler.model.User;
import com.budgetbutler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security has its own internal "UserDetails" concept it uses to check passwords
 * and manage permissions. This class is the translator: "when Spring Security asks for a
 * user by email, here's how to find one using OUR User table".
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        // Spring Security's built-in User class just needs: username, password hash, and roles.
        // We don't use roles/authorities in this MVP, so we pass an empty list.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList()
        );
    }
}
