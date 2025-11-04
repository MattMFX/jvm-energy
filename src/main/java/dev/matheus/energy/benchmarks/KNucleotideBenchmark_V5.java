package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.util.*;
import java.util.concurrent.*;

/**
 * K-Nucleotide benchmark variant #5 from the Computer Language Benchmarks Game.
 * Contributed by Daryl Griffith - uses ForkJoinPool and optimized data structures.
 * 
 * Source: benchmarksgame/knucleotide/knucleotide.java-5.java
 */
public class KNucleotideBenchmark_V5 implements BenchmarkAlgorithm {
    
    private byte[] nucleotides;
    
    @Override
    public String getName() {
        return "knucleotide_v5";
    }
    
    @Override
    public String getDescription() {
        return "DNA sequence analysis (ForkJoin variant 5)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        return 250_000;
    }
    
    @Override
    public void setup(int size) {
        // Generate synthetic DNA sequence for benchmarking
        Random rand = new Random(42);
        byte[] bases = {0, 1, 2, 3}; // Encoded A, C, G, T as 0,1,2,3
        nucleotides = new byte[size];
        for (int i = 0; i < size; i++) {
            nucleotides[i] = bases[rand.nextInt(4)];
        }
    }
    
    @Override
    public Object execute(int size) throws Exception {
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        Map<Key, Value> MAP = new ConcurrentHashMap<>();
        
        int[] sequences1 = {2, 1};
        int[] sequences2 = {18, 12, 6, 4, 3};
        String[] specificSequences = {"GGT", "GGTA", "GGTATT", "GGTATTTTAATT", "GGTATTTTAATTTATAGT"};
        
        // Count sequences
        countSequences(pool, MAP, sequences1);
        
        List<Map.Entry<Key, Value>> sequence1 = new ArrayList<>();
        List<Map.Entry<Key, Value>> sequence2 = new ArrayList<>();
        
        for (Map.Entry<Key, Value> entry : MAP.entrySet()) {
            int leadingZeros = Long.numberOfLeadingZeros(entry.getKey().key);
            if (leadingZeros == 61) {
                sequence1.add(entry);
            } else if (leadingZeros == 59) {
                sequence2.add(entry);
            }
        }
        
        StringBuilder result = new StringBuilder();
        result.append(printSequence(sequence1));
        result.append(printSequence(sequence2));
        
        countSequences(pool, MAP, sequences2);
        
        Key key = new Key();
        for (String sequence : specificSequences) {
            key.setHash(sequence);
            Value val = MAP.get(key);
            result.append(val != null ? val.count : 0).append('\t').append(sequence).append('\n');
        }
        
        pool.shutdown();
        return result.toString();
    }
    
    private byte translate(byte b) {
        return (byte) ((b >> 1) & 3);
    }
    
    private void countSequences(ForkJoinPool pool, Map<Key, Value> MAP, int[] sequences) {
        Future[] futures = new Future[sequences.length];
        int i = 0;
        
        for (int sequence : sequences) {
            futures[i] = pool.submit(new KNucleotideTask(sequence, MAP));
            i++;
        }
        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                // Ignore
            }
        }
    }
    
    private void updateHashtable(int sequence, Map<Key, Value> MAP) {
        int sequenceTop = nucleotides.length - sequence + 1;
        Key key = new Key();
        Value value;
        
        for (int i = 0; i < sequenceTop; i++) {
            key.setHash(i, sequence);
            value = MAP.get(key);
            if (value == null) {
                value = new Value();
                value.count = 1;
                MAP.put(key, value);
                key = new Key();
            } else {
                value.count++;
            }
        }
    }
    
    private String printSequence(List<Map.Entry<Key, Value>> sequence) {
        int sum = 0;
        
        Collections.sort(sequence, new Comparator<Map.Entry<Key, Value>>() {
            @Override
            public int compare(Map.Entry<Key, Value> entry1, Map.Entry<Key, Value> entry2) {
                if (entry2.getValue().count != entry1.getValue().count) {
                    return entry2.getValue().count - entry1.getValue().count;
                }
                return entry1.getKey().toString().compareTo(entry2.getKey().toString());
            }
        });
        
        for (Map.Entry<Key, Value> entry : sequence) {
            sum += entry.getValue().count;
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Key, Value> entry : sequence) {
            sb.append(String.format("%s %.3f\n", entry.getKey(), entry.getValue().count * 100f / sum));
        }
        sb.append('\n');
        return sb.toString();
    }
    
    private class KNucleotideTask extends ForkJoinTask<Object> {
        private final int sequence;
        private final Map<Key, Value> MAP;
        Object rawResult;
        
        public KNucleotideTask(int sequence, Map<Key, Value> MAP) {
            this.sequence = sequence;
            this.MAP = MAP;
        }
        
        @Override
        public Object getRawResult() {
            return rawResult;
        }
        
        @Override
        protected void setRawResult(Object value) {
            rawResult = value;
        }
        
        @Override
        protected boolean exec() {
            updateHashtable(sequence, MAP);
            setRawResult(new Object());
            return true;
        }
    }
    
    private class Key {
        long key;
        
        void setHash(int offset, int length) {
            key = 1;
            for (int i = offset + length - 1; i >= offset; i--) {
                key = (key << 2) | nucleotides[i];
            }
        }
        
        void setHash(String species) {
            key = 1;
            for (int i = species.length() - 1; i >= 0; i--) {
                key = (key << 2) | translate((byte) species.charAt(i));
            }
        }
        
        @Override
        public int hashCode() {
            return (int) key;
        }
        
        @Override
        public boolean equals(Object obj) {
            final Key other = (Key) obj;
            return key == other.key;
        }
        
        @Override
        public String toString() {
            char[] name = new char[(63 - Long.numberOfLeadingZeros(key)) / 2];
            long temp = key;
            
            for (int i = 0; temp > 1; temp >>= 2, i++) {
                name[i] = (char) (((temp & 3) << 1) | 'A');
                if (name[i] == 'E') {
                    name[i] = 'T';
                }
            }
            return new String(name);
        }
    }
    
    private static class Value {
        int count;
    }
}

