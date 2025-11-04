package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import dev.matheus.energy.SortingAlgorithms;
import java.util.Arrays;
import java.util.Random;

/**
 * QuickSort algorithm benchmark.
 */
public class QuickSortBenchmark implements BenchmarkAlgorithm {
    
    private int[] data;
    private Random random;
    
    @Override
    public String getName() {
        return "quick_sort";
    }
    
    @Override
    public String getDescription() {
        return "QuickSort algorithm";
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
        SortingAlgorithms.quickSort(copy);
        return copy;
    }
}

