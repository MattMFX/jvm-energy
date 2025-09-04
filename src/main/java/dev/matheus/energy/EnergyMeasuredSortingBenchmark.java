package dev.matheus.energy;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import dev.matheus.energy.jrapl.EnergyCheckUtils;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// Minimal integration of jRAPL. If jRAPL is unavailable on this CPU, we print a note.
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Thread)
public class EnergyMeasuredSortingBenchmark {

    @Param({"10000"})
    int arraySize;

    int[] data;
    Random random;

    // Track if energy measurement is available
    private boolean energyAvailable;

    @Setup(Level.Trial)
    public void setup() {
        random = new Random(123);
        data = new int[arraySize];
        try {
            // Test if jRAPL is available by calling a simple method
            EnergyCheckUtils.GetSocketNum();
            energyAvailable = true;
            System.out.println("jRAPL energy measurement enabled");
        } catch (Throwable t) {
            energyAvailable = false;
            System.out.println("jRAPL energy measurement not available: " + t.getMessage());
        }
    }

    @Setup(Level.Invocation)
    public void prepareData() {
        for (int i = 0; i < data.length; i++) data[i] = random.nextInt();
    }

    @Benchmark
    public void quick_sort_energy(Blackhole bh) throws Exception {
        int[] copy = Arrays.copyOf(data, data.length);
        double energyConsumed = Double.NaN;
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                SortingAlgorithms.quickSort(copy);
                String after = EnergyCheckUtils.getEnergyStats();
                energyConsumed = parsePackageEnergyDelta(before, after);
                System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "quick_sort", arraySize, energyConsumed);
                EnergyLog.append("quick_sort", arraySize, energyConsumed);
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
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                SortingAlgorithms.bubbleSort(copy);
                String after = EnergyCheckUtils.getEnergyStats();
                energyConsumed = parsePackageEnergyDelta(before, after);
                System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "bubble_sort", arraySize, energyConsumed);
                EnergyLog.append("bubble_sort", arraySize, energyConsumed);
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
        if (energyAvailable) {
            try {
                EnergyCheckUtils.init();
                String before = EnergyCheckUtils.getEnergyStats();
                SortingAlgorithms.mergeSort(copy);
                String after = EnergyCheckUtils.getEnergyStats();
                energyConsumed = parsePackageEnergyDelta(before, after);
                System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "merge_sort", arraySize, energyConsumed);
                EnergyLog.append("merge_sort", arraySize, energyConsumed);
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
        // Parse package energy (first value in comma-separated list from getEnergyStats)
        try {
            double b = Double.parseDouble(before.split(",")[0].trim());
            double a = Double.parseDouble(after.split(",")[0].trim());
            return a - b;
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}


