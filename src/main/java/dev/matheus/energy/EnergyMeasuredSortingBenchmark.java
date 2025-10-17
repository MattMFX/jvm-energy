package dev.matheus.energy;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.IterationType;
import dev.matheus.energy.jrapl.EnergyCheckUtils;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 50, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class EnergyMeasuredSortingBenchmark {

    @Param({"10000"})
    int arraySize;

    int[] data;
    Random random;

    // Track if energy measurement is available
    private boolean energyAvailable;
    
    // Track current iteration type
    private volatile IterationType currentIterationType;

    @Setup(Level.Trial)
    public void setup() {
        random = new Random(123);
        data = new int[arraySize];
        try {
            EnergyCheckUtils.GetSocketNum();
            energyAvailable = true;
            System.out.println("jRAPL energy measurement enabled");
        } catch (Throwable t) {
            energyAvailable = false;
            System.out.println("jRAPL energy measurement not available: " + t.getMessage());
        }
    }

    @Setup(Level.Iteration)
    public void setupIteration(IterationParams iterationParams) {
        currentIterationType = iterationParams.getType();
        System.out.printf("DEBUG: Setting up iteration type: %s%n", currentIterationType);
    }

    @Setup(Level.Invocation)
    public void prepareData() {
        for (int i = 0; i < data.length; i++) data[i] = random.nextInt();
    }

    @Benchmark
    public void quick_sort_energy(Blackhole bh) throws Exception {
        int[] copy = Arrays.copyOf(data, data.length);
        double energyConsumed = Double.NaN;
        double executionTimeMs = Double.NaN;
        boolean isMeasurementPhase = currentIterationType != null && currentIterationType.equals(IterationType.MEASUREMENT);
        
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                long startTime = System.nanoTime();
                SortingAlgorithms.quickSort(copy);
                long endTime = System.nanoTime();
                String after = EnergyCheckUtils.getEnergyStats();
                
                energyConsumed = parsePackageEnergyDelta(before, after);
                executionTimeMs = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
                
                if (isMeasurementPhase) {
                    System.out.printf("energy,algo=%s,size=%d,joules=%.9f,time_ms=%.3f%n", "quick_sort", arraySize, energyConsumed, executionTimeMs);
                    EnergyLog.append("quick_sort", arraySize, energyConsumed, executionTimeMs);
                } else {
                    System.out.printf("warmup,algo=%s,size=%d,joules=%.9f,time_ms=%.3f (WARMUP - NOT LOGGED)%n", "quick_sort", arraySize, energyConsumed, executionTimeMs);
                }
            } catch (Throwable t) {
                System.err.println("Energy measurement failed: " + t.getMessage());
                long startTime = System.nanoTime();
                SortingAlgorithms.quickSort(copy);
                long endTime = System.nanoTime();
                executionTimeMs = (endTime - startTime) / 1_000_000.0;
            }
        } else {
            long startTime = System.nanoTime();
            SortingAlgorithms.quickSort(copy);
            long endTime = System.nanoTime();
            executionTimeMs = (endTime - startTime) / 1_000_000.0;
        }
        bh.consume(copy);
        bh.consume(energyConsumed);
        bh.consume(executionTimeMs);
    }

    @Benchmark
    public void bubble_sort_energy(Blackhole bh) throws Exception {
        int[] copy = Arrays.copyOf(data, data.length);
        double energyConsumed = Double.NaN;
        double executionTimeMs = Double.NaN;
        boolean isMeasurementPhase = currentIterationType != null && currentIterationType.equals(IterationType.MEASUREMENT);

        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                long startTime = System.nanoTime();
                SortingAlgorithms.bubbleSort(copy);
                long endTime = System.nanoTime();
                String after = EnergyCheckUtils.getEnergyStats();
                
                energyConsumed = parsePackageEnergyDelta(before, after);
                executionTimeMs = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
                
                if (isMeasurementPhase) {
                    System.out.printf("energy,algo=%s,size=%d,joules=%.9f,time_ms=%.3f%n", "bubble_sort", arraySize, energyConsumed, executionTimeMs);
                    EnergyLog.append("bubble_sort", arraySize, energyConsumed, executionTimeMs);
                } else {
                    System.out.printf("warmup,algo=%s,size=%d,joules=%.9f,time_ms=%.3f (WARMUP - NOT LOGGED)%n", "bubble_sort", arraySize, energyConsumed, executionTimeMs);
                }
            } catch (Throwable t) {
                System.err.println("Energy measurement failed: " + t.getMessage());
                long startTime = System.nanoTime();
                SortingAlgorithms.bubbleSort(copy);
                long endTime = System.nanoTime();
                executionTimeMs = (endTime - startTime) / 1_000_000.0;
            }
        } else {
            long startTime = System.nanoTime();
            SortingAlgorithms.bubbleSort(copy);
            long endTime = System.nanoTime();
            executionTimeMs = (endTime - startTime) / 1_000_000.0;
        }
        bh.consume(copy);
        bh.consume(energyConsumed);
        bh.consume(executionTimeMs);
    }

    @Benchmark
    public void merge_sort_energy(Blackhole bh) throws Exception {
        int[] copy = Arrays.copyOf(data, data.length);
        double energyConsumed = Double.NaN;
        double executionTimeMs = Double.NaN;
        boolean isMeasurementPhase = currentIterationType != null && currentIterationType.equals(IterationType.MEASUREMENT);
        
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                long startTime = System.nanoTime();
                SortingAlgorithms.mergeSort(copy);
                long endTime = System.nanoTime();
                String after = EnergyCheckUtils.getEnergyStats();
                
                energyConsumed = parsePackageEnergyDelta(before, after);
                executionTimeMs = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
                
                if (isMeasurementPhase) {
                    System.out.printf("energy,algo=%s,size=%d,joules=%.9f,time_ms=%.3f%n", "merge_sort", arraySize, energyConsumed, executionTimeMs);
                    EnergyLog.append("merge_sort", arraySize, energyConsumed, executionTimeMs);
                } else {
                    System.out.printf("warmup,algo=%s,size=%d,joules=%.9f,time_ms=%.3f (WARMUP - NOT LOGGED)%n", "merge_sort", arraySize, energyConsumed, executionTimeMs);
                }
            } catch (Throwable t) {
                System.err.println("Energy measurement failed: " + t.getMessage());
                long startTime = System.nanoTime();
                SortingAlgorithms.mergeSort(copy);
                long endTime = System.nanoTime();
                executionTimeMs = (endTime - startTime) / 1_000_000.0;
            }
        } else {
            long startTime = System.nanoTime();
            SortingAlgorithms.mergeSort(copy);
            long endTime = System.nanoTime();
            executionTimeMs = (endTime - startTime) / 1_000_000.0;
        }
        bh.consume(copy);
        bh.consume(energyConsumed);
        bh.consume(executionTimeMs);
    }

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


