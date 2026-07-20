package com.budgetbutler.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A minimal rate limiter kept entirely in memory - no Redis or external service needed,
 * which keeps this beginner-friendly and free to run. It tracks failed attempts per key
 * (we use the client's IP + the email they tried) and blocks further attempts once a
 * threshold is hit, until the time window passes.
 *
 * NOTE: because this is in-memory, it resets if the app restarts, and doesn't share state
 * across multiple server instances. For a small personal project that's a fine trade-off;
 * a production app with real scale would use something like Redis instead.
 */
@Component
public class RateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000; // 15 minutes

    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /** Returns true if this key has exceeded the allowed attempts within the current window. */
    public boolean isBlocked(String key) {
        AttemptRecord record = attempts.get(key);
        if (record == null) return false;

        if (Instant.now().toEpochMilli() - record.windowStart > WINDOW_MILLIS) {
            attempts.remove(key); // window expired - forget the old attempts
            return false;
        }
        return record.count.get() >= MAX_ATTEMPTS;
    }

    /** Call this every time an attempt fails (e.g. wrong password). */
    public void recordFailedAttempt(String key) {
        attempts.compute(key, (k, existing) -> {
            long now = Instant.now().toEpochMilli();
            if (existing == null || now - existing.windowStart > WINDOW_MILLIS) {
                return new AttemptRecord(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    /** Call this on a successful login/register so a past failed streak doesn't linger. */
    public void reset(String key) {
        attempts.remove(key);
    }

    private static class AttemptRecord {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(1);

        AttemptRecord(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
