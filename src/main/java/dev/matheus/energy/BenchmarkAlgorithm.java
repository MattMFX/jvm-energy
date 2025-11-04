package dev.matheus.energy;

/**
 * Base interface for all benchmark algorithms.
 * Each algorithm must implement this interface to be measured for energy consumption.
 */
public interface BenchmarkAlgorithm {
    
    /**
     * Execute the algorithm with the given input parameter.
     * 
     * @param input The input parameter for the algorithm (e.g., array size, iteration count, etc.)
     * @return A result object that can be consumed by JMH's Blackhole to prevent dead code elimination
     * @throws Exception if the algorithm execution fails
     */
    Object execute(int input) throws Exception;
    
    /**
     * Get the name of the algorithm (used for logging and identification).
     * 
     * @return The algorithm name
     */
    String getName();
    
    /**
     * Get a brief description of the algorithm.
     * 
     * @return The algorithm description
     */
    default String getDescription() {
        return getName();
    }
    
    /**
     * Optional setup method called before each execution.
     * Can be used to initialize state that should not be measured.
     * 
     * @param input The input parameter
     */
    default void setup(int input) {
        // Default: no setup needed
    }
    
    /**
     * Get the recommended input size for this algorithm.
     * This allows each algorithm to define what "size" means and what's reasonable.
     * 
     * @param requestedSize The size requested by the user (e.g., from -s flag)
     * @return The actual size to use for this algorithm
     */
    default int getEffectiveSize(int requestedSize) {
        // Default: use requested size as-is
        return requestedSize;
    }
}

