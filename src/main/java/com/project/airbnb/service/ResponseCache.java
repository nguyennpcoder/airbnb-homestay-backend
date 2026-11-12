package com.project.airbnb.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Small in-memory TTL cache for public, read-only responses.
 *
 * The app runs on a single free Render instance with a remote Supabase
 * database, where even a warm search costs 2–3 seconds because it runs several
 * queries in sequence. Caching the assembled response for a short time makes
 * repeat visits and homepage ISR revalidation effectively free.
 *
 * This is intentionally dependency-free: a bounded ConcurrentHashMap with
 * lazy expiry is enough for a handful of public endpoints.
 */
@Component
public class ResponseCache {

    private record Entry(Object value, long expiresAt) {
    }

    /** Above this size, expired entries are swept on the next write. */
    private static final int SWEEP_THRESHOLD = 400;

    /** Hard cap so unusual query combinations cannot grow memory forever. */
    private static final int MAX_ENTRIES = 800;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, long ttlMillis, Supplier<T> loader) {
        long now = System.currentTimeMillis();
        Entry current = entries.get(key);
        if (current != null && current.expiresAt() > now) {
            return (T) current.value();
        }

        T value = loader.get();
        if (value != null) {
            if (entries.size() > SWEEP_THRESHOLD) {
                entries.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
            }
            if (entries.size() >= MAX_ENTRIES) {
                entries.clear();
            }
            entries.put(key, new Entry(value, now + ttlMillis));
        }
        return value;
    }

    public void invalidate(String key) {
        entries.remove(key);
    }

    public void invalidateAll() {
        entries.clear();
    }
}
