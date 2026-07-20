package com.budgetbutler.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * This class is the "master switch" for security rules:
 * - Which URLs are public (login/register) vs. require a valid token (everything else)
 * - That we use stateless sessions (no server-side session storage - the JWT IS the session)
 * - Where our JwtAuthFilter fits into Spring's request-processing pipeline
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Let Spring Security know about our CORS rules (defined in CorsConfig) -
                // otherwise it would block the Angular app's requests before they even reach our filter.
                .cors(cors -> {})

                // We're building a stateless REST API, not a traditional website with forms,
                // so we don't need Spring's default CSRF protection (which is cookie-based).
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Anyone can call register/login without already having a token.
                        .requestMatchers("/api/auth/**").permitAll()
                        // Stripe calls this directly (not through the browser) - it proves
                        // its identity via a signature, not a JWT, so it must stay public.
                        .requestMatchers("/api/subscription/webhook").permitAll()
                        // Every other /api/** endpoint requires a valid JWT.
                        .anyRequest().authenticated()
                )

                // STATELESS = the server keeps no memory of "who is logged in" between requests.
                // Every request must carry its own proof of identity (the JWT).
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authenticationProvider(authenticationProvider())

                // Insert our custom filter BEFORE Spring's default username/password filter,
                // since we're authenticating via JWT instead of a login form.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt turns a plain-text password into an irreversible hash. Used for both storing and checking passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Tells Spring Security how to find users (via our CustomUserDetailsService) and check their passwords. */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** Exposes Spring's AuthenticationManager so our AuthController can use it to verify login attempts. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
