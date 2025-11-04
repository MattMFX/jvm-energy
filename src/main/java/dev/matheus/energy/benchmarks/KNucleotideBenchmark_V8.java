package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.util.*;
import java.util.stream.*;

/**
 * K-Nucleotide benchmark variant #8 from the Computer Language Benchmarks Game.
 * Naive transliteration from bearophile's program using streams.
 * 
 * Source: benchmarksgame/knucleotide/knucleotide.java-8.java
 * Contributed by Isaac Gouy
 */
public class KNucleotideBenchmark_V8 implements BenchmarkAlgorithm {
    
    private String sequence;
    
    @Override
    public String getName() {
        return "knucleotide_v8";
    }
    
    @Override
    public String getDescription() {
        return "DNA sequence analysis (naive stream variant 8)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        return 250_000;
    }
    
    @Override
    public void setup(int size) {
        // Generate synthetic DNA sequence for benchmarking
        Random rand = new Random(42);
        char[] bases = {'A', 'C', 'G', 'T'};
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(bases[rand.nextInt(4)]);
        }
        sequence = sb.toString();
    }
    
    @Override
    public Object execute(int size) throws Exception {
        StringBuilder result = new StringBuilder();
        
        for (int i : Arrays.asList(1, 2)) {  
            for (String s : sortedFreq(i, sequence)) {      
                result.append(s).append('\n'); 
            }
            result.append('\n');          
        }
        
        for (String code : Arrays.asList("GGT", "GGTA", "GGTATT",
                "GGTATTTTAATT", "GGTATTTTAATTTATAGT")) {       
            result.append(specificCount(code, sequence)).append('\t').append(code).append('\n');                        
        }
        
        return result.toString();
    }
    
    private HashMap<String, Integer> baseCounts(int bases, String seq) {
        HashMap<String, Integer> counts = new HashMap<>();  
        final int size = seq.length() + 1 - bases;     
        for (int i = 0; i < size; i++) {
            String nucleo = seq.substring(i, i + bases);
            Integer v;
            if ((v = counts.get(nucleo)) != null) {
                counts.put(nucleo, v + 1);
            } else {
                counts.put(nucleo, 1);
            }                           
        }      
        return counts;
    }
    
    private List<String> sortedFreq(int bases, String seq) {
        final int size = seq.length() + 1 - bases;
        return baseCounts(bases, seq).entrySet()     
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .map(e -> String.format("%s %.3f", 
                    e.getKey(), 100.0 * e.getValue() / size))
            .toList();
    }   
    
    private int specificCount(String code, String seq) {
        return baseCounts(code.length(), seq).getOrDefault(code, 0);   
    }
}

