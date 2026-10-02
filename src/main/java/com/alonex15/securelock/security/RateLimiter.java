package com.alonex15.securelock.security;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Limita los paquetes custom por jugador a N por segundo (ventana fija de 1 s). */
public final class RateLimiter<K> {
    private final Map<K, Window> windows = new HashMap<>();
    private final LongSupplier clock;
    private int maxPerSecond;

    public RateLimiter(int maxPerSecond, LongSupplier clock) {
        this.clock = clock;
        setMaxPerSecond(maxPerSecond);
    }

    public RateLimiter(int maxPerSecond) {
        this(maxPerSecond, System::currentTimeMillis);
    }

    public void setMaxPerSecond(int maxPerSecond) {
        this.maxPerSecond = Math.max(1, maxPerSecond);
    }

    public boolean tryAcquire(K key) {
        long now = clock.getAsLong();
        Window window = windows.get(key);
        if (window == null) {
            window = new Window(now);
            windows.put(key, window);
        } else if (now - window.start >= 1000) {
            window.start = now;
            window.count = 0;
        }
        return ++window.count <= maxPerSecond;
    }

    public void forget(K key) {
        windows.remove(key);
    }

    private static final class Window {
        long start;
        int count;

        Window(long start) {
            this.start = start;
        }
    }
}
