package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.util.*;
import java.util.concurrent.*;

/**
 * K-Nucleotide benchmark from the Computer Language Benchmarks Game.
 * Performs DNA sequence analysis using hash tables and parallel processing.
 * 
 * Source: benchmarksgame/knucleotide/knucleotide.java-3.java
 * Contributed by James McIlree
 * ByteString code thanks to Matthieu Bentot and The Anh Tran
 * Modified by Andy Fingerhut
 */
public class KNucleotideBenchmark_V3 implements BenchmarkAlgorithm {
    
    private byte[] sequence;
    
    @Override
    public String getName() {
        return "knucleotide";
    }
    
    @Override
    public String getDescription() {
        return "DNA sequence analysis with hashtables";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // K-Nucleotide: 250,000 base pairs
        return 250_000;
    }
    
    @Override
    public void setup(int size) {
        // Generate synthetic DNA sequence for benchmarking
        // In the real benchmark, this would be read from input
        Random rand = new Random(42);
        byte[] nucleotides = {'A', 'C', 'G', 'T'};
        sequence = new byte[size];
        for (int i = 0; i < size; i++) {
            sequence[i] = nucleotides[rand.nextInt(4)];
        }
    }
    
    @Override
    public Object execute(int size) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        
        int[] fragmentLengths = {1, 2, 3, 4, 6, 12, 18};
        List<Future<Map<ByteString, ByteString>>> futures = 
            pool.invokeAll(createFragmentTasks(sequence, fragmentLengths));
        pool.shutdown();
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(writeFrequencies(sequence.length, futures.get(0).get()));
        sb.append(writeFrequencies(sequence.length - 1, 
            sumTwoMaps(futures.get(1).get(), futures.get(2).get())));
        
        String[] nucleotideFragments = {"ggt", "ggta", "ggtatt", 
            "ggtattttaatt", "ggtattttaatttatagt"};
        for (String nucleotideFragment : nucleotideFragments) {
            sb.append(writeCount(futures, nucleotideFragment));
        }
        
        return sb.toString();
    }
    
    // Internal implementation
    
    private static ArrayList<Callable<Map<ByteString, ByteString>>> createFragmentTasks(
            final byte[] sequence, int[] fragmentLengths) {
        ArrayList<Callable<Map<ByteString, ByteString>>> tasks = new ArrayList<>();
        for (int fragmentLength : fragmentLengths) {
            for (int index = 0; index < fragmentLength; index++) {
                final int offset = index;
                final int finalFragmentLength = fragmentLength;
                tasks.add(new Callable<Map<ByteString, ByteString>>() {
                    public Map<ByteString, ByteString> call() {
                        return createFragmentMap(sequence, offset, finalFragmentLength);
                    }
                });
            }
        }
        return tasks;
    }
    
    private static Map<ByteString, ByteString> createFragmentMap(
            byte[] sequence, int offset, int fragmentLength) {
        HashMap<ByteString, ByteString> map = new HashMap<>();
        int lastIndex = sequence.length - fragmentLength + 1;
        ByteString key = new ByteString(fragmentLength);
        for (int index = offset; index < lastIndex; index += fragmentLength) {
            key.calculateHash(sequence, index);
            ByteString fragment = map.get(key);
            if (fragment != null) {
                fragment.count++;
            } else {
                map.put(key, key);
                key = new ByteString(fragmentLength);
            }
        }
        return map;
    }
    
    private static Map<ByteString, ByteString> sumTwoMaps(
            Map<ByteString, ByteString> map1, Map<ByteString, ByteString> map2) {
        for (Map.Entry<ByteString, ByteString> entry : map2.entrySet()) {
            ByteString sum = map1.get(entry.getKey());
            if (sum != null) {
                sum.count += entry.getValue().count;
            } else {
                map1.put(entry.getKey(), entry.getValue());
            }
        }
        return map1;
    }
    
    private static String writeFrequencies(float totalCount, 
            Map<ByteString, ByteString> frequencies) {
        SortedSet<ByteString> list = new TreeSet<>(frequencies.values());
        StringBuilder sb = new StringBuilder();
        for (ByteString k : list) {
            sb.append(String.format("%s %.3f\n", 
                k.toString().toUpperCase(), 
                (float) (k.count) * 100.0f / totalCount));
        }
        return sb.append('\n').toString();
    }
    
    private static String writeCount(
            List<Future<Map<ByteString, ByteString>>> futures, 
            String nucleotideFragment) throws Exception {
        ByteString key = new ByteString(nucleotideFragment.length());
        key.calculateHash(nucleotideFragment.getBytes(), 0);
        
        int count = 0;
        for (Future<Map<ByteString, ByteString>> future : futures) {
            ByteString temp = future.get().get(key);
            if (temp != null) count += temp.count;
        }
        
        return count + "\t" + nucleotideFragment.toUpperCase() + '\n';
    }
    
    private static final class ByteString implements Comparable<ByteString> {
        public int hash, count = 1;
        public final byte bytes[];
        
        public ByteString(int size) {
            bytes = new byte[size];
        }
        
        public void calculateHash(byte k[], int offset) {
            int temp = 0;
            for (int i = 0; i < bytes.length; i++) {
                byte b = k[offset + i];
                bytes[i] = b;
                temp = temp * 31 + b;
            }
            hash = temp;
        }
        
        public int hashCode() {
            return hash;
        }
        
        public boolean equals(Object obj) {
            return Arrays.equals(bytes, ((ByteString) obj).bytes);
        }
        
        public int compareTo(ByteString other) {
            if (other.count != count) {
                return other.count - count;
            } else {
                return toString().compareTo(other.toString());
            }
        }
        
        public String toString() {
            return new String(bytes);
        }
    }
}

