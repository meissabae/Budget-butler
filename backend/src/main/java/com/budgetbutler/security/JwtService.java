package com.budgetbutler.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * A JWT ("JSON Web Token") is a string with 3 parts separated by dots:
 * header.payload.signature
 *
 * The payload contains info like "who is this user" and "when does this expire".
 * The signature is created using a secret key that only OUR server knows.
 * That means: anyone can READ a JWT's payload, but nobody can FAKE one without our secret key -
 * if they change even one character, the signature won't match anymore and we'll reject it.
 *
 * This is why we don't need to look anything up in the database to check if someone is logged in -
 * we just verify the signature is valid and hasn't expired.
 */
@Service
public class JwtService {

    // In a real production app, this secret would come from an environment variable,
    // not be hardcoded. For learning purposes, we keep it in application.properties.
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /** Creates a brand-new signed token for this user's email. Called right after login/register succeeds. */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)       // "subject" = who this token belongs to
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Reads the email back out of a token (after verifying its signature). */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Returns true if the token's signature is valid AND it hasn't expired yet. */
    public boolean isTokenValid(String token, String expectedEmail) {
        String email = extractEmail(token);
        return email.equals(expectedEmail) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token) // this line throws an exception if the signature is invalid
                .getBody();
        return resolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
