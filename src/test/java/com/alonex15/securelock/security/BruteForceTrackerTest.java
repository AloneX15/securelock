package com.alonex15.securelock.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BruteForceTrackerTest {
    private final AtomicLong clock = new AtomicLong(1_000_000);
    private BruteForceTracker<String> tracker;

    @BeforeEach
    void setUp() {
        tracker = new BruteForceTracker<>(3, 30_000, clock::get);
    }

    @Test
    void locksAfterMaxAttempts() {
        assertFalse(tracker.recordFailure("p1"));
        assertFalse(tracker.recordFailure("p1"));
        assertFalse(tracker.isLocked("p1"));
        assertTrue(tracker.recordFailure("p1"), "el tercer fallo bloquea");
        assertTrue(tracker.isLocked("p1"));
        assertEquals(30_000, tracker.remainingLockout("p1"));
    }

    @Test
    void lockoutExpires() {
        for (int i = 0; i < 3; i++) {
            tracker.recordFailure("p1");
        }
        clock.addAndGet(29_999);
        assertTrue(tracker.isLocked("p1"));
        clock.addAndGet(1);
        assertFalse(tracker.isLocked("p1"));
        assertEquals(0, tracker.failures("p1"), "tras el bloqueo se empieza de cero");
    }

    @Test
    void failuresDuringLockoutKeepItLocked() {
        for (int i = 0; i < 3; i++) {
            tracker.recordFailure("p1");
        }
        assertTrue(tracker.recordFailure("p1"));
        assertEquals(30_000, tracker.remainingLockout("p1"), "fallar durante el bloqueo no lo alarga ni lo reinicia");
    }

    @Test
    void successResetsCounter() {
        tracker.recordFailure("p1");
        tracker.recordFailure("p1");
        tracker.recordSuccess("p1");
        assertEquals(0, tracker.failures("p1"));
        assertFalse(tracker.recordFailure("p1"));
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) {
            tracker.recordFailure("p1");
        }
        assertTrue(tracker.isLocked("p1"));
        assertFalse(tracker.isLocked("p2"));
    }

    @Test
    void cleanupRemovesExpiredEntries() {
        for (int i = 0; i < 3; i++) {
            tracker.recordFailure("p1");
        }
        clock.addAndGet(60_000);
        tracker.cleanup();
        assertEquals(0, tracker.size());
    }

    @Test
    void zeroLockoutNeverLocks() {
        tracker.configure(1, 0);
        assertFalse(tracker.recordFailure("p1"));
        assertFalse(tracker.isLocked("p1"));
    }
}
