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
        // BENCHMARKS GAME ALGORITHMS - ALL VARIANTS
        // ========================================
        
        // Binary Trees variants
        BenchmarkRegistry.register(new BinaryTreesBenchmark_V2(), true);
        BenchmarkRegistry.register(new BinaryTreesBenchmark_V3(), true);
        BenchmarkRegistry.register(new BinaryTreesBenchmark_V7(), true);
        
        // Fannkuch Redux variants
        BenchmarkRegistry.register(new FannkuchReduxBenchmark_V1(), true);
        BenchmarkRegistry.register(new FannkuchReduxBenchmark_V2(), true);
        BenchmarkRegistry.register(new FannkuchReduxBenchmark_V8(), true);
        
        // Fasta variants
        BenchmarkRegistry.register(new FastaBenchmark_V2(), true);
        BenchmarkRegistry.register(new FastaBenchmark_V6(), true);
        BenchmarkRegistry.register(new FastaBenchmark_V8(), true);
        
        // K-Nucleotide variants
        BenchmarkRegistry.register(new KNucleotideBenchmark_V3(), true);
        BenchmarkRegistry.register(new KNucleotideBenchmark_V5(), true);
        BenchmarkRegistry.register(new KNucleotideBenchmark_V8(), true);
        
        // Mandelbrot variants
        BenchmarkRegistry.register(new MandelbrotBenchmark_V1(), true);
        BenchmarkRegistry.register(new MandelbrotBenchmark_V2(), true);
        BenchmarkRegistry.register(new MandelbrotBenchmark_V3(), true);
        
        // N-Body variants
        BenchmarkRegistry.register(new NBodyBenchmark_V1(), true);
        BenchmarkRegistry.register(new NBodyBenchmark_V5(), true);
        BenchmarkRegistry.register(new NBodyBenchmark_V8(), true);
        
        // Spectral Norm variants
        BenchmarkRegistry.register(new SpectralNormBenchmark_V1(), true);
        BenchmarkRegistry.register(new SpectralNormBenchmark_V2(), true);
        BenchmarkRegistry.register(new SpectralNormBenchmark_V3(), true);
        
        // Add more benchmarks here as needed...
        
        initialized = true;
        
        System.err.println("Registered benchmarks: " + BenchmarkRegistry.getAllNames());
        System.err.println("Enabled benchmarks: " + BenchmarkRegistry.getEnabledNames());
    }
    
    /**
     * Reset the configuration (mainly for testing purposes).
     */
    public static synchronized void reset() {
        BenchmarkRegistry.clear();
        initialized = false;
    }
}

