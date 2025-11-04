package dev.matheus.energy;

import java.util.Set;
import java.util.TreeSet;

/**
 * Simple utility to list available benchmark algorithms.
 * Used by the shell script to discover which algorithms to run.
 */
public class BenchmarkLister {
    
    public static void main(String[] args) {
        // Initialize the registry
        BenchmarkConfig.initialize();
        
        // Determine which algorithms to list
        Set<String> algorithmsToList;
        
        if (args.length > 0) {
            // Filter by prefix(es) provided as arguments
            String[] prefixes = args[0].split(",");
            for (int i = 0; i < prefixes.length; i++) {
                prefixes[i] = prefixes[i].trim();
            }
            BenchmarkRegistry.enableOnly(prefixes);
            algorithmsToList = BenchmarkRegistry.getEnabledNames();
        } else {
            // List all enabled algorithms (from BenchmarkConfig defaults)
            algorithmsToList = BenchmarkRegistry.getEnabledNames();
        }
        
        // Sort for consistent output
        Set<String> sortedAlgorithms = new TreeSet<>(algorithmsToList);
        
        // Print one algorithm per line
        for (String algoName : sortedAlgorithms) {
            System.out.println(algoName);
        }
    }
}

