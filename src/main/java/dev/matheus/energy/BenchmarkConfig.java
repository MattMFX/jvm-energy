package dev.matheus.energy;

import dev.matheus.energy.benchmarks.*;

/**
 * Configuration class for registering and enabling benchmarks.
 * 
 * To add a new benchmark:
 * 1. Create a class implementing BenchmarkAlgorithm
 * 2. Register it here with BenchmarkRegistry.register(new YourBenchmark(), true/false)
 * 
 * To enable/disable a benchmark:
 * - Change the second parameter in register() to true (enabled) or false (disabled)
 * - Or call BenchmarkRegistry.enable()/disable() programmatically
 */
public final class BenchmarkConfig {
    
    private static boolean initialized = false;
    
    /**
     * Initialize and register all available benchmarks.
     * This method is idempotent - calling it multiple times has no additional effect.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        // ========================================
        // SORTING ALGORITHMS
        // ========================================
        BenchmarkRegistry.register(new QuickSortBenchmark(), false);
        BenchmarkRegistry.register(new BubbleSortBenchmark(), false);
        BenchmarkRegistry.register(new MergeSortBenchmark(), false);
        
        // ========================================
        // BENCHMARKS GAME ALGORITHMS
        // ========================================
        BenchmarkRegistry.register(new NBodyBenchmark(), true);
        BenchmarkRegistry.register(new SpectralNormBenchmark(), true);
        BenchmarkRegistry.register(new BinaryTreesBenchmark(), true);
        BenchmarkRegistry.register(new MandelbrotBenchmark(), true);
        BenchmarkRegistry.register(new FannkuchReduxBenchmark(), true);
        BenchmarkRegistry.register(new FastaBenchmark(), true);
        BenchmarkRegistry.register(new KNucleotideBenchmark(), true);
        
        // Add more benchmarks here as needed...
        
        initialized = true;
        
        System.out.println("Registered benchmarks: " + BenchmarkRegistry.getAllNames());
        System.out.println("Enabled benchmarks: " + BenchmarkRegistry.getEnabledNames());
    }
    
    /**
     * Reset the configuration (mainly for testing purposes).
     */
    public static synchronized void reset() {
        BenchmarkRegistry.clear();
        initialized = false;
    }
}

