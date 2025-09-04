package dev.matheus.energy;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

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

    // Lazy reflection to avoid hard dependency in case library fails to init on non-supported CPUs
    private Object energyUtils;
    private java.lang.reflect.Method initMethod;
    private java.lang.reflect.Method getStatsMethod;
    private java.lang.reflect.Method deallocMethod;
    private boolean energyAvailable;

    @Setup(Level.Trial)
    public void setup() {
        random = new Random(123);
        data = new int[arraySize];
        try {
            Class<?> utils = Class.forName("jrapl.EnergyCheckUtils");
            energyUtils = utils;
            initMethod = utils.getMethod("init");
            getStatsMethod = utils.getMethod("getEnergyStats");
            deallocMethod = utils.getMethod("dealloc");
            energyAvailable = true;
        } catch (Throwable t) {
            energyAvailable = false;
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
            initMethod.invoke(energyUtils);
            String before = (String) getStatsMethod.invoke(energyUtils);
            SortingAlgorithms.quickSort(copy);
            String after = (String) getStatsMethod.invoke(energyUtils);
            try { deallocMethod.invoke(energyUtils); } catch (Throwable ignored) {}
            energyConsumed = parsePackageEnergyDelta(before, after);
            System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "quick_sort", arraySize, energyConsumed);
            EnergyLog.append("quick_sort", arraySize, energyConsumed);
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
            initMethod.invoke(energyUtils);
            String before = (String) getStatsMethod.invoke(energyUtils);
            SortingAlgorithms.bubbleSort(copy);
            String after = (String) getStatsMethod.invoke(energyUtils);
            try { deallocMethod.invoke(energyUtils); } catch (Throwable ignored) {}
            energyConsumed = parsePackageEnergyDelta(before, after);
            System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "bubble_sort", arraySize, energyConsumed);
            EnergyLog.append("bubble_sort", arraySize, energyConsumed);
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
            initMethod.invoke(energyUtils);
            String before = (String) getStatsMethod.invoke(energyUtils);
            SortingAlgorithms.mergeSort(copy);
            String after = (String) getStatsMethod.invoke(energyUtils);
            try { deallocMethod.invoke(energyUtils); } catch (Throwable ignored) {}
            energyConsumed = parsePackageEnergyDelta(before, after);
            System.out.printf("energy,algo=%s,size=%d,joules=%.9f%n", "merge_sort", arraySize, energyConsumed);
            EnergyLog.append("merge_sort", arraySize, energyConsumed);
        } else {
            SortingAlgorithms.mergeSort(copy);
        }
        bh.consume(copy);
        bh.consume(energyConsumed);
    }

    private static double parsePackageEnergyDelta(String before, String after) {
        // Typical jRAPL string: "<pkg0,dram0,pkg1,dram1,...>" in joules. We'll parse first field as pkg0.
        try {
            double b = Double.parseDouble(before.split(",")[0].replace("[", "").trim());
            double a = Double.parseDouble(after.split(",")[0].replace("[", "").trim());
            return a - b;
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}


