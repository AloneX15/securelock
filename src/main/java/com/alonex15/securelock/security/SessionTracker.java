package com.alonex15.securelock.security;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Recuerda durante unos segundos que un jugador ya introdujo el código correcto de un bloque. */
public final class SessionTracker<K> {
    private final Map<K, Long> expiries = new HashMap<>();
    private final LongSupplier clock;

    public SessionTracker(LongSupplier clock) {
        this.clock = clock;
    }

    public SessionTracker() {
        this(System::currentTimeMillis);
    }

    public void grant(K key, long durationMillis) {
        if (durationMillis > 0) {
            expiries.put(key, clock.getAsLong() + durationMillis);
        }
    }

    public boolean isActive(K key) {
        Long expiry = expiries.get(key);
        if (expiry == null) {
            return false;
        }
        if (expiry <= clock.getAsLong()) {
            expiries.remove(key);
            return false;
        }
        return true;
    }

    public void revoke(K key) {
        expiries.remove(key);
    }

    public void cleanup() {
        long now = clock.getAsLong();
        expiries.values().removeIf(expiry -> expiry <= now);
    }

    public void clear() {
        expiries.clear();
    }
}
