package com.budgetbutler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * By default, browsers block a webpage on one "origin" (e.g. http://localhost:4200, our Angular app)
 * from calling an API on a different origin (e.g. http://localhost:8080, our Spring Boot app).
 * This class tells Spring Boot: "it's OK, allow requests coming from the Angular app".
 *
 * The allowed origin is read from a property instead of hardcoded, so deploying to production
 * (a real domain instead of localhost) only needs an environment variable change - no code edit,
 * no rebuild. See application.properties for the default value used during local development.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors-allowed-origin}")
    private String allowedOrigin;


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedOrigins(Arrays.asList(
                "https://budgetbutler.netlify.app",
                "http://localhost:4200"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

