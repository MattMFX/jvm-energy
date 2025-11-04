package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Fasta benchmark variant #8 from the Computer Language Benchmarks Game.
 * Naive transliteration from Drake Diedrich's C program.
 * 
 * Source: benchmarksgame/fasta/fasta.java-8.java
 * Contributed by Isaac Gouy
 */
public class FastaBenchmark_V8 implements BenchmarkAlgorithm {
    
    private static final int IM = 139968;
    private static final int IA = 3877;
    private static final int IC = 29573;
    private static final int SEED = 42;
    
    private int seed = SEED;
    
    @Override
    public String getName() {
        return "fasta_v8";
    }
    
    @Override
    public String getDescription() {
        return "DNA sequence generation (naive variant 8)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        return 2_500_000;
    }
    
    @Override
    public void setup(int input) {
        seed = SEED; // Reset seed for each execution
    }
    
    @Override
    public Object execute(int n) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        repeatFasta(ALU, n * 2, out);
        randomFasta(iub, iubP, n * 3, out);
        randomFasta(homosapiens, homosapiensP, n * 5, out);
        
        return out.toByteArray();
    }
    
    private double fastaRand(double max) {
        seed = (seed * IA + IC) % IM;
        return max * seed / IM;
    }
    
    private static final String ALU =
        "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG" +
        "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA" +
        "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT" +
        "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA" +
        "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG" +
        "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC" +
        "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA";
    
    private static final String iub = "acgtBDHKMNRSVWY";
    private static final double[] iubP = {
        0.27, 0.12, 0.12, 0.27, 
        0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02 
    };
    
    private static final String homosapiens = "acgt";
    private static final double[] homosapiensP = {
        0.3029549426680,
        0.1979883004921,
        0.1975473066391,
        0.3015094502008
    };   
    
    private static final int LINELEN = 60;
    
    private void repeatFasta(String seq, int n, ByteArrayOutputStream out) {
        int len = seq.length();
        int i;
        StringBuilder b = new StringBuilder(); 
        for (i = 0; i < n; i++) {
            b.append(seq.charAt(i % len));   
            if (i % LINELEN == LINELEN - 1) {
                out.write(b.toString().getBytes(), 0, b.length());
                out.write('\n');
                b.setLength(0);  
            }
        }
        if (i % LINELEN != 0) {
            out.write(b.toString().getBytes(), 0, b.length());
            out.write('\n');
        }
    }
    
    private void randomFasta(String seq, double[] probability, int n, ByteArrayOutputStream out) {
        int len = seq.length();
        int i, j;
        StringBuilder b = new StringBuilder();    
        for (i = 0; i < n; i++) {
            double v = fastaRand(1.0);        
            for (j = 0; j < len - 1; j++) {  
                v -= probability[j];
                if (v < 0) break;      
            }
            b.append(seq.charAt(j));        
            if (i % LINELEN == LINELEN - 1) {
                out.write(b.toString().getBytes(), 0, b.length());
                out.write('\n');
                b.setLength(0);         
            }         
        }
        if (i % LINELEN != 0) {
            out.write(b.toString().getBytes(), 0, b.length());
            out.write('\n');
        }
    }
}

