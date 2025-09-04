package dev.matheus.energy;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
@State(Scope.Thread)
public class SortingBenchmark {

    @Param({"1000", "10000"})
    int arraySize;

    int[] data;
    Random random;

    @Setup(Level.Trial)
    public void setup() {
        random = new Random(42);
        data = new int[arraySize];
    }

    @Setup(Level.Invocation)
    public void prepareData() {
        for (int i = 0; i < data.length; i++) data[i] = random.nextInt();
    }

    @Benchmark
    public void bubble_sort(Blackhole bh) {
        int[] copy = Arrays.copyOf(data, data.length);
        SortingAlgorithms.bubbleSort(copy);
        bh.consume(copy);
    }

    @Benchmark
    public void quick_sort(Blackhole bh) {
        int[] copy = Arrays.copyOf(data, data.length);
        SortingAlgorithms.quickSort(copy);
        bh.consume(copy);
    }

    @Benchmark
    public void merge_sort(Blackhole bh) {
        int[] copy = Arrays.copyOf(data, data.length);
        SortingAlgorithms.mergeSort(copy);
        bh.consume(copy);
    }
}


