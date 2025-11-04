package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Fasta benchmark from the Computer Language Benchmarks Game.
 * Generates DNA sequences using pseudo-random number generation.
 * 
 * Source: benchmarksgame/fasta/fasta.java-2.java
 * Modified by Mehmet D. AKIN
 */
public class FastaBenchmark_V2 implements BenchmarkAlgorithm {
    
    @Override
    public String getName() {
        return "fasta_v2";
    }
    
    @Override
    public String getDescription() {
        return "DNA sequence generation";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Fasta: 2.5 million base pairs
        return 2_500_000;
    }
    
    @Override
    public Object execute(int n) throws Exception {
        makeCumulative(HomoSapiens);
        makeCumulative(IUB);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        makeRepeatFasta("ONE", "Homo sapiens alu", ALU, n * 2, out);
        makeRandomFasta("TWO", "IUB ambiguity codes", IUB, n * 3, out);
        makeRandomFasta("THREE", "Homo sapiens frequency", HomoSapiens, n * 5, out);
        
        return out.toByteArray();
    }
    
    // Constants and helper classes
    private static final int IM = 139968;
    private static final int IA = 3877;
    private static final int IC = 29573;
    private static int last = 42;
    private static final int LINE_LENGTH = 60;
    
    private static final String ALU = 
              "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG"
            + "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA"
            + "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT"
            + "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA"
            + "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG"
            + "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC"
            + "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA";
    private static final byte[] ALUB = ALU.getBytes();
    
    private static final Frequency[] IUB = new Frequency[] {
        new Frequency('a', 0.27), 
        new Frequency('c', 0.12),
        new Frequency('g', 0.12), 
        new Frequency('t', 0.27),
        new Frequency('B', 0.02), 
        new Frequency('D', 0.02),
        new Frequency('H', 0.02), 
        new Frequency('K', 0.02),
        new Frequency('M', 0.02), 
        new Frequency('N', 0.02),
        new Frequency('R', 0.02), 
        new Frequency('S', 0.02),
        new Frequency('V', 0.02), 
        new Frequency('W', 0.02),
        new Frequency('Y', 0.02)
    };
    
    private static final Frequency[] HomoSapiens = new Frequency[] {
        new Frequency('a', 0.3029549426680d),
        new Frequency('c', 0.1979883004921d),
        new Frequency('g', 0.1975473066391d),
        new Frequency('t', 0.3015094502008d)
    };
    
    private static double random(double max) {
        last = (last * IA + IC) % IM;
        return max * last / IM;
    }
    
    private static void makeCumulative(Frequency[] a) {
        double cp = 0.0;
        for (int i = 0; i < a.length; i++) {
            cp += a[i].p;
            a[i].p = cp;
        }
    }
    
    private static byte selectRandom(Frequency[] a) {
        int len = a.length;
        double r = random(1.0);
        for (int i = 0; i < len; i++) {
            if (r < a[i].p) {
                return a[i].c;
            }
        }
        return a[len - 1].c;
    }
    
    private static void makeRandomFasta(String id, String desc, Frequency[] a, int n, OutputStream writer) 
            throws IOException {
        int BUFFER_SIZE = 1024;
        int index = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        int m = 0;
        String descStr = ">" + id + " " + desc + '\n';
        writer.write(descStr.getBytes());
        
        while (n > 0) {
            if (n < LINE_LENGTH) m = n;
            else m = LINE_LENGTH;
            
            if (BUFFER_SIZE - index < m) {
                writer.write(buffer, 0, index);
                index = 0;
            }
            
            for (int i = 0; i < m; i++) {
                buffer[index++] = selectRandom(a);
            }
            buffer[index++] = '\n';
            n -= LINE_LENGTH;
        }
        
        if (index != 0) {
            writer.write(buffer, 0, index);
        }
    }
    
    private static void makeRepeatFasta(String id, String desc, String alu, int n, OutputStream writer) 
            throws IOException {
        int BUFFER_SIZE = 1024;
        int index = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        int m = 0;
        int k = 0;
        int kn = ALUB.length;
        String descStr = ">" + id + " " + desc + '\n';
        writer.write(descStr.getBytes());
        
        while (n > 0) {
            if (n < LINE_LENGTH) m = n;
            else m = LINE_LENGTH;
            
            if (BUFFER_SIZE - index < m) {
                writer.write(buffer, 0, index);
                index = 0;
            }
            
            for (int i = 0; i < m; i++) {
                if (k == kn) k = 0;
                buffer[index++] = ALUB[k];
                k++;
            }
            buffer[index++] = '\n';
            n -= LINE_LENGTH;
        }
        
        if (index != 0) {
            writer.write(buffer, 0, index);
        }
    }
    
    private static class Frequency {
        public byte c;
        public double p;
        
        public Frequency(char c, double p) {
            this.c = (byte) c;
            this.p = p;
        }
    }
}

