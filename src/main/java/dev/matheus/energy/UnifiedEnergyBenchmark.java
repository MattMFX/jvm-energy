package dev.matheus.energy;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.IterationType;
import dev.matheus.energy.jrapl.EnergyCheckUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Simplified unified energy measurement benchmark.
 * 
 * This benchmark runs EXACTLY ONE algorithm per execution.
 * The shell script (run_energy_benchmark.sh) orchestrates running
 * this benchmark multiple times, once per algorithm.
 * 
 * Usage:
 * - Use -Dbenchmark.filter=<algorithm_name> to specify which algorithm to run
 * - The benchmark verifies exactly one algorithm is enabled
 * 
 * Features:
 * - Single algorithm execution per run (gets full JMH timing)
 * - Energy measurement using jRAPL
 * - Graceful fallback if energy measurement is unavailable
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
    
    // The single algorithm to benchmark
    private BenchmarkAlgorithm algorithm;
    
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
        List<BenchmarkAlgorithm> enabledBenchmarks = BenchmarkRegistry.getEnabledBenchmarks();
        
        if (enabledBenchmarks.isEmpty()) {
            throw new RuntimeException("No benchmarks are enabled! Use -Dbenchmark.filter=<name> to specify an algorithm.");
        }
        
        if (enabledBenchmarks.size() > 1) {
            System.err.println("WARNING: Multiple benchmarks are enabled. Only the first one will run.");
            System.err.println("Enabled: " + BenchmarkRegistry.getEnabledNames());
            System.err.println("Consider using -Dbenchmark.filter=<exact_name> to run a specific algorithm.");
        }
        
        // Use only the first enabled benchmark
        algorithm = enabledBenchmarks.get(0);
        
        // Check if energy measurement is available
        try {
            EnergyCheckUtils.GetSocketNum();
            energyAvailable = true;
            System.out.println("jRAPL energy measurement enabled");
        } catch (Throwable t) {
            energyAvailable = false;
            System.out.println("jRAPL energy measurement not available: " + t.getMessage());
        }
        
        System.out.println("Running benchmark: " + algorithm.getName() + " - " + algorithm.getDescription());
    }
    
    @Setup(Level.Iteration)
    public void setupIteration(IterationParams iterationParams) {
        currentIterationType = iterationParams.getType();
    }
    
    @Setup(Level.Invocation)
    public void setupInvocation() {
        int effectiveSize = algorithm.getEffectiveSize(size);
        algorithm.setup(effectiveSize);
    }
    
    /**
     * Main benchmark method that runs the single enabled algorithm.
     * This method gets the full JMH timing allocation.
     */
    @Benchmark
    public void runBenchmark(Blackhole bh) throws Exception {
        boolean isMeasurementPhase = currentIterationType != null && 
                                     currentIterationType.equals(IterationType.MEASUREMENT);
        
        double energyConsumed = Double.NaN;
        double executionTimeMs = Double.NaN;
        Object result = null;
        int effectiveSize = algorithm.getEffectiveSize(size);
        
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                long startTime = System.nanoTime();
                
                result = algorithm.execute(effectiveSize);
                
                long endTime = System.nanoTime();
                String after = EnergyCheckUtils.getEnergyStats();
                
                energyConsumed = parsePackageEnergyDelta(before, after);
                executionTimeMs = (endTime - startTime) / 1_000_000.0;
                
                if (isMeasurementPhase) {
                    System.out.printf("energy,algo=%s,size=%d,joules=%.9f,time_ms=%.3f%n", 
                                    algorithm.getName(), effectiveSize, energyConsumed, executionTimeMs);
                    EnergyLog.append(algorithm.getName(), effectiveSize, energyConsumed, executionTimeMs);
                } else {
                    System.out.printf("warmup,algo=%s,size=%d,joules=%.9f,time_ms=%.3f (WARMUP - NOT LOGGED)%n", 
                                    algorithm.getName(), effectiveSize, energyConsumed, executionTimeMs);
                }
            } catch (Throwable t) {
                System.err.println("Energy measurement failed for " + algorithm.getName() + ": " + t.getMessage());
                long startTime = System.nanoTime();
                result = algorithm.execute(effectiveSize);
                long endTime = System.nanoTime();
                executionTimeMs = (endTime - startTime) / 1_000_000.0;
            }
        } else {
            // No energy measurement available, just run the benchmark
            long startTime = System.nanoTime();
            result = algorithm.execute(effectiveSize);
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

