package dev.matheus.energy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all benchmark algorithms.
 * Use this registry to enable/disable benchmarks easily.
 */
public final class BenchmarkRegistry {
    
    private static final Map<String, BenchmarkAlgorithm> REGISTRY = new ConcurrentHashMap<>();
    private static final Set<String> ENABLED = ConcurrentHashMap.newKeySet();
    
    /**
     * Register a benchmark algorithm.
     * 
     * @param algorithm The algorithm to register
     * @param enabled Whether the algorithm should be enabled by default
     */
    public static void register(BenchmarkAlgorithm algorithm, boolean enabled) {
        String name = algorithm.getName();
        REGISTRY.put(name, algorithm);
        if (enabled) {
            ENABLED.add(name);
        }
    }
    
    /**
     * Enable a benchmark by name.
     * 
     * @param name The algorithm name
     */
    public static void enable(String name) {
        if (REGISTRY.containsKey(name)) {
            ENABLED.add(name);
        }
    }
    
    /**
     * Disable a benchmark by name.
     * 
     * @param name The algorithm name
     */
    public static void disable(String name) {
        ENABLED.remove(name);
    }
    
    /**
     * Check if a benchmark is enabled.
     * 
     * @param name The algorithm name
     * @return true if enabled, false otherwise
     */
    public static boolean isEnabled(String name) {
        return ENABLED.contains(name);
    }
    
    /**
     * Get all registered benchmark algorithms.
     * 
     * @return Collection of all registered algorithms
     */
    public static Collection<BenchmarkAlgorithm> getAllBenchmarks() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
    
    /**
     * Get all enabled benchmark algorithms.
     * 
     * @return List of enabled algorithms
     */
    public static List<BenchmarkAlgorithm> getEnabledBenchmarks() {
        List<BenchmarkAlgorithm> result = new ArrayList<>();
        for (Map.Entry<String, BenchmarkAlgorithm> entry : REGISTRY.entrySet()) {
            if (ENABLED.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }
    
    /**
     * Get a specific benchmark by name.
     * 
     * @param name The algorithm name
     * @return The algorithm, or null if not found
     */
    public static BenchmarkAlgorithm getBenchmark(String name) {
        return REGISTRY.get(name);
    }
    
    /**
     * Get all registered benchmark names.
     * 
     * @return Set of all algorithm names
     */
    public static Set<String> getAllNames() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
    
    /**
     * Get all enabled benchmark names.
     * 
     * @return Set of enabled algorithm names
     */
    public static Set<String> getEnabledNames() {
        return Collections.unmodifiableSet(ENABLED);
    }
    
    /**
     * Enable only benchmarks that match a given prefix.
     * All other benchmarks will be disabled.
     * 
     * @param prefix The prefix to match (e.g., "nbody" matches "nbody", "nbody_v5", "nbody_v8")
     */
    public static void enableOnly(String prefix) {
        ENABLED.clear();
        for (String name : REGISTRY.keySet()) {
            if (name.startsWith(prefix)) {
                ENABLED.add(name);
            }
        }
    }
    
    /**
     * Enable only benchmarks that match any of the given prefixes.
     * All other benchmarks will be disabled.
     * 
     * @param prefixes The prefixes to match
     */
    public static void enableOnly(String... prefixes) {
        ENABLED.clear();
        for (String prefix : prefixes) {
            for (String name : REGISTRY.keySet()) {
                if (name.startsWith(prefix)) {
                    ENABLED.add(name);
                }
            }
        }
    }
    
    /**
     * Enable all registered benchmarks.
     */
    public static void enableAll() {
        ENABLED.clear();
        ENABLED.addAll(REGISTRY.keySet());
    }
    
    /**
     * Disable all benchmarks.
     */
    public static void disableAll() {
        ENABLED.clear();
    }
    
    /**
     * Clear all registrations (useful for testing).
     */
    public static void clear() {
        REGISTRY.clear();
        ENABLED.clear();
    }
}

