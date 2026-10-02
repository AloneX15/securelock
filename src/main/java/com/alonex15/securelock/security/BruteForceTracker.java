package com.alonex15.securelock.security;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Cuenta intentos fallidos por clave (jugador + bloque). Tras {@code maxAttempts} fallos
 * la clave queda bloqueada {@code lockoutMillis}. No es thread-safe: se usa solo en el hilo del servidor.
 */
public final class BruteForceTracker<K> {
    private final Map<K, Entry> entries = new HashMap<>();
    private final LongSupplier clock;
    private int maxAttempts;
    private long lockoutMillis;

    public BruteForceTracker(int maxAttempts, long lockoutMillis, LongSupplier clock) {
        this.clock = clock;
        configure(maxAttempts, lockoutMillis);
    }

    public BruteForceTracker(int maxAttempts, long lockoutMillis) {
        this(maxAttempts, lockoutMillis, System::currentTimeMillis);
    }

    public void configure(int maxAttempts, long lockoutMillis) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockoutMillis = Math.max(0, lockoutMillis);
    }

    /** Milisegundos de bloqueo restantes, o 0 si la clave puede intentarlo. */
    public long remainingLockout(K key) {
        Entry entry = entries.get(key);
        if (entry == null || entry.lockedUntil == 0) {
            return 0;
        }
        long remaining = entry.lockedUntil - clock.getAsLong();
        if (remaining <= 0) {
            entries.remove(key);
            return 0;
        }
        return remaining;
    }

    public boolean isLocked(K key) {
        return remainingLockout(key) > 0;
    }

    /** Registra un fallo. Devuelve {@code true} si la clave queda bloqueada tras este fallo. */
    public boolean recordFailure(K key) {
        if (isLocked(key)) {
            return true;
        }
        Entry entry = entries.computeIfAbsent(key, k -> new Entry());
        entry.lockedUntil = 0;
        entry.failures++;
        if (entry.failures >= maxAttempts) {
            entry.failures = 0;
            if (lockoutMillis > 0) {
                entry.lockedUntil = clock.getAsLong() + lockoutMillis;
                return true;
            }
        }
        return false;
    }

    public void recordSuccess(K key) {
        entries.remove(key);
    }

    public int failures(K key) {
        Entry entry = entries.get(key);
        return entry == null ? 0 : entry.failures;
    }

    /** Elimina entradas caducadas para que el mapa no crezca sin límite. */
    public void cleanup() {
        long now = clock.getAsLong();
        entries.values().removeIf(e -> e.lockedUntil != 0 && e.lockedUntil <= now);
    }

    public int size() {
        return entries.size();
    }

    private static final class Entry {
        int failures;
        long lockedUntil;
    }
}
