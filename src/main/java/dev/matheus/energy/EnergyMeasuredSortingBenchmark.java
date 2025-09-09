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
        boolean isMeasurementPhase = currentIterationType != null && currentIterationType.equals(IterationType.MEASUREMENT);
        
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                SortingAlgorithms.quickSort(copy);
                String after = EnergyCheckUtils.getEnergyStats();
                energyConsumed = parsePackageEnergyDelta(before, after);
                
                if (isMeasurementPhase) {
                    System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "quick_sort", arraySize, energyConsumed);
                    EnergyLog.append("quick_sort", arraySize, energyConsumed);
                } else {
                    System.out.printf("warmup,algo=%s,size=%d,joules=%.9f (WARMUP - NOT LOGGED)%n", "quick_sort", arraySize, energyConsumed);
                }
            } catch (Throwable t) {
                System.err.println("Energy measurement failed: " + t.getMessage());
                SortingAlgorithms.quickSort(copy);
            }
        } else {
            SortingAlgorithms.quickSort(copy);
        }
        bh.consume(copy);
        bh.consume(energyConsumed);
    }

    @Benchmark
    public void bubble_sort_energy(Blackhole bh) throws Exception {
        int[] copy = Arrays.copyOf(data, data.length);
        double energyConsumed = Double.NaN;
        boolean isMeasurementPhase = currentIterationType != null && currentIterationType.equals(IterationType.MEASUREMENT);

        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                SortingAlgorithms.bubbleSort(copy);
                String after = EnergyCheckUtils.getEnergyStats();
                energyConsumed = parsePackageEnergyDelta(before, after);
                
                if (isMeasurementPhase) {
                    System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "bubble_sort", arraySize, energyConsumed);
                    EnergyLog.append("bubble_sort", arraySize, energyConsumed);
                } else {
                    System.out.printf("warmup,algo=%s,size=%d,joules=%.9f (WARMUP - NOT LOGGED)%n", "bubble_sort", arraySize, energyConsumed);
                }
            } catch (Throwable t) {
                System.err.println("Energy measurement failed: " + t.getMessage());
                SortingAlgorithms.bubbleSort(copy);
            }
        } else {
            SortingAlgorithms.bubbleSort(copy);
        }
        bh.consume(copy);
        bh.consume(energyConsumed);
    }

    @Benchmark
    public void merge_sort_energy(Blackhole bh) throws Exception {
        int[] copy = Arrays.copyOf(data, data.length);
        double energyConsumed = Double.NaN;
        boolean isMeasurementPhase = currentIterationType != null && currentIterationType.equals(IterationType.MEASUREMENT);
        
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                SortingAlgorithms.mergeSort(copy);
                String after = EnergyCheckUtils.getEnergyStats();
                energyConsumed = parsePackageEnergyDelta(before, after);
                
                if (isMeasurementPhase) {
                    System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "merge_sort", arraySize, energyConsumed);
                    EnergyLog.append("merge_sort", arraySize, energyConsumed);
                } else {
                    System.out.printf("warmup,algo=%s,size=%d,joules=%.9f (WARMUP - NOT LOGGED)%n", "merge_sort", arraySize, energyConsumed);
                }
            } catch (Throwable t) {
                System.err.println("Energy measurement failed: " + t.getMessage());
                SortingAlgorithms.mergeSort(copy);
            }
        } else {
            SortingAlgorithms.mergeSort(copy);
        }
        bh.consume(copy);
        bh.consume(energyConsumed);
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


