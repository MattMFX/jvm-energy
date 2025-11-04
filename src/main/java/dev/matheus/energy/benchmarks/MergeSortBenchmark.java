package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import dev.matheus.energy.SortingAlgorithms;
import java.util.Arrays;
import java.util.Random;

/**
 * MergeSort algorithm benchmark.
 */
public class MergeSortBenchmark implements BenchmarkAlgorithm {
    
    private int[] data;
    private Random random;
    
    @Override
    public String getName() {
        return "merge_sort";
    }
    
    @Override
    public String getDescription() {
        return "MergeSort algorithm";
    }
    
    @Override
    public void setup(int input) {
        random = new Random(123);
        data = new int[input];
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextInt();
        }
    }
    
    @Override
    public Object execute(int input) throws Exception {
        int[] copy = Arrays.copyOf(data, data.length);
        SortingAlgorithms.mergeSort(copy);
        return copy;
    }
}

