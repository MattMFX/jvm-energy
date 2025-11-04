package dev.matheus.energy;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.IterationType;
import dev.matheus.energy.jrapl.EnergyCheckUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Unified energy measurement benchmark for all registered algorithms.
 * 
 * This benchmark automatically discovers and runs all enabled benchmarks
 * from the BenchmarkRegistry. To add or remove benchmarks, modify the
 * BenchmarkConfig class.
 * 
 * Features:
 * - Automatic discovery of registered benchmarks
 * - Energy measurement using jRAPL
 * - Graceful fallback if energy measurement is unavailable
 * - Modular design for easy addition/removal of benchmarks
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 50, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class UnifiedEnergyBenchmark {
    
    @Param({"10000"})
    int size;
    
    // Track if energy measurement is available
    private boolean energyAvailable;
    
    // Track current iteration type
    private volatile IterationType currentIterationType;
    
    // List of enabled benchmarks
    private List<BenchmarkAlgorithm> enabledBenchmarks;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        // Initialize benchmark registry
        BenchmarkConfig.initialize();
        
        // Check if benchmark filter is specified via system property
        String benchmarkFilter = System.getProperty("benchmark.filter");
        if (benchmarkFilter != null && !benchmarkFilter.isEmpty()) {
            System.out.println("Applying benchmark filter: " + benchmarkFilter);
            String[] prefixes = benchmarkFilter.split(",");
            for (int i = 0; i < prefixes.length; i++) {
                prefixes[i] = prefixes[i].trim();
            }
            BenchmarkRegistry.enableOnly(prefixes);
        }
        
        // Get enabled benchmarks
        enabledBenchmarks = BenchmarkRegistry.getEnabledBenchmarks();
        
        if (enabledBenchmarks.isEmpty()) {
            System.err.println("WARNING: No benchmarks are enabled!");
        }
        
        // Check if energy measurement is available
        try {
            EnergyCheckUtils.GetSocketNum();
            energyAvailable = true;
            System.out.println("jRAPL energy measurement enabled");
        } catch (Throwable t) {
            energyAvailable = false;
            System.out.println("jRAPL energy measurement not available: " + t.getMessage());
        }
        
        System.out.println("Benchmarks to run: " + enabledBenchmarks.size());
        for (BenchmarkAlgorithm algo : enabledBenchmarks) {
            System.out.println("  - " + algo.getName() + ": " + algo.getDescription());
        }
    }
    
    @Setup(Level.Iteration)
    public void setupIteration(IterationParams iterationParams) {
        currentIterationType = iterationParams.getType();
    }
    
    @Setup(Level.Invocation)
    public void setupInvocation() {
        // Setup each benchmark before invocation
        for (BenchmarkAlgorithm algo : enabledBenchmarks) {
            int effectiveSize = algo.getEffectiveSize(size);
            algo.setup(effectiveSize);
        }
    }
    
    /**
     * Main benchmark method that runs all enabled benchmarks.
     * This method measures energy consumption for each algorithm.
     */
    @Benchmark
    public void runAllBenchmarks(Blackhole bh) throws Exception {
        boolean isMeasurementPhase = currentIterationType != null && 
                                     currentIterationType.equals(IterationType.MEASUREMENT);
        
        // Run each enabled benchmark
        for (BenchmarkAlgorithm algo : enabledBenchmarks) {
            runSingleBenchmark(algo, bh, isMeasurementPhase);
        }
    }
    
    /**
     * Run a single benchmark with energy measurement.
     */
    private void runSingleBenchmark(BenchmarkAlgorithm algo, Blackhole bh, boolean isMeasurementPhase) 
            throws Exception {
        double energyConsumed = Double.NaN;
        double executionTimeMs = Double.NaN;
        Object result = null;
        int effectiveSize = algo.getEffectiveSize(size);
        
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                long startTime = System.nanoTime();
                
                result = algo.execute(effectiveSize);
                
                long endTime = System.nanoTime();
                String after = EnergyCheckUtils.getEnergyStats();
                
                energyConsumed = parsePackageEnergyDelta(before, after);
                executionTimeMs = (endTime - startTime) / 1_000_000.0;
                
                if (isMeasurementPhase) {
                    System.out.printf("energy,algo=%s,size=%d,joules=%.9f,time_ms=%.3f%n", 
                                    algo.getName(), effectiveSize, energyConsumed, executionTimeMs);
                    EnergyLog.append(algo.getName(), effectiveSize, energyConsumed, executionTimeMs);
                } else {
                    System.out.printf("warmup,algo=%s,size=%d,joules=%.9f,time_ms=%.3f (WARMUP - NOT LOGGED)%n", 
                                    algo.getName(), effectiveSize, energyConsumed, executionTimeMs);
                }
            } catch (Throwable t) {
                System.err.println("Energy measurement failed for " + algo.getName() + ": " + t.getMessage());
                long startTime = System.nanoTime();
                result = algo.execute(effectiveSize);
                long endTime = System.nanoTime();
                executionTimeMs = (endTime - startTime) / 1_000_000.0;
            }
        } else {
            // No energy measurement available, just run the benchmark
            long startTime = System.nanoTime();
            result = algo.execute(effectiveSize);
            long endTime = System.nanoTime();
            executionTimeMs = (endTime - startTime) / 1_000_000.0;
        }
        
        // Consume results to prevent dead code elimination
        bh.consume(result);
        bh.consume(energyConsumed);
        bh.consume(executionTimeMs);
    }
    
    /**
     * Parse the energy delta from jRAPL output.
     */
    private static double parsePackageEnergyDelta(String before, String after) {
        try {
            double b = Double.parseDouble(before.split(",")[0].trim());
            double a = Double.parseDouble(after.split(",")[0].trim());
            return a - b;
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}

