package com.takumistudios.securemod.security;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterAndSessionTest {
    private final AtomicLong clock = new AtomicLong(5_000);

    @Test
    void rateLimiterAllowsNPerSecond() {
        RateLimiter<String> limiter = new RateLimiter<>(3, clock::get);
        assertTrue(limiter.tryAcquire("p"));
        assertTrue(limiter.tryAcquire("p"));
        assertTrue(limiter.tryAcquire("p"));
        assertFalse(limiter.tryAcquire("p"), "el cuarto paquete en el mismo segundo se descarta");
        assertTrue(limiter.tryAcquire("other"), "cada jugador tiene su propio límite");
        clock.addAndGet(1000);
        assertTrue(limiter.tryAcquire("p"), "nueva ventana");
    }

    @Test
    void rateLimiterSurvivesSpam() {
        RateLimiter<String> limiter = new RateLimiter<>(10, clock::get);
        int accepted = 0;
        for (int i = 0; i < 100_000; i++) {
            if (limiter.tryAcquire("spammer")) {
                accepted++;
            }
        }
        assertTrue(accepted == 10);
    }

    @Test
    void sessionExpires() {
        SessionTracker<String> sessions = new SessionTracker<>(clock::get);
        assertFalse(sessions.isActive("p@chest"));
        sessions.grant("p@chest", 10_000);
        assertTrue(sessions.isActive("p@chest"));
        clock.addAndGet(9_999);
        assertTrue(sessions.isActive("p@chest"));
        clock.addAndGet(1);
        assertFalse(sessions.isActive("p@chest"));
    }

    @Test
    void sessionRevokeAndZeroDuration() {
        SessionTracker<String> sessions = new SessionTracker<>(clock::get);
        sessions.grant("a", 0);
        assertFalse(sessions.isActive("a"), "duración 0 = sin sesión");
        sessions.grant("b", 5_000);
        sessions.revoke("b");
        assertFalse(sessions.isActive("b"));
    }

    @Test
    void accessModeParsing() {
        assertTrue(AccessMode.byId("PUBLIC") == AccessMode.PUBLIC);
        assertTrue(AccessMode.byId("shared") == AccessMode.SHARED);
        assertTrue(AccessMode.byId("hacker") == AccessMode.PRIVATE, "valores desconocidos -> privado (fallo seguro)");
        assertTrue(AccessMode.PUBLIC.next() == AccessMode.PRIVATE);
    }
}
