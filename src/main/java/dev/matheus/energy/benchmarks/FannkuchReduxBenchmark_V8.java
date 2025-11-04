package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;

/**
 * FannkuchRedux benchmark variant #8 from the Computer Language Benchmarks Game.
 * Naive transliteration from Rex Kerr's Scala program.
 * 
 * Source: benchmarksgame/fannkuchredux/fannkuchredux.java-8.java
 * Contributed by Isaac Gouy
 */
public class FannkuchReduxBenchmark_V8 implements BenchmarkAlgorithm {
    
    @Override
    public String getName() {
        return "fannkuchredux_v8";
    }
    
    @Override
    public String getDescription() {
        return "Fannkuch-Redux indexed array access (naive variant 8)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // FannkuchRedux: n=12
        return 12;
    }
    
    @Override
    public Object execute(int n) throws Exception {
        return fannkuch(n);
    }
    
    private int fannkuch(int n) {
        int[] perm1 = new int[n];   
        for (int i = 0; i < n; i++) perm1[i] = i;      
        int[] perm = new int[n];
        int[] count = new int[n];       
        int f = 0, flips = 0, nperm = 0, checksum = 0;      
        int i, k, r;      
    
        r = n;
        while (r > 0) { 
            i = 0;  
            while (r != 1) { count[r-1] = r; r -= 1; }   
            while (i < n) { perm[i] = perm1[i]; i += 1; }     
          
            // Count flips and update max and checksum
            f = 0;
            k = perm[0];  
            while (k != 0) {
                i = 0;  
                while (2*i < k) {          
                    int t = perm[i]; perm[i] = perm[k-i]; perm[k-i] = t;  
                    i += 1;           
                }
                k = perm[0];
                f += 1;   
            }         
            if (f > flips) flips = f;         
            if ((nperm & 0x1) == 0) checksum += f; else checksum -= f;
     
            // Use incremental change to generate another permutation   
            boolean more = true;
            while (more) {   
                if (r == n) {
                    return flips; 
                }
                int p0 = perm1[0];
                i = 0;
                while (i < r) {
                    int j = i + 1;
                    perm1[i] = perm1[j];
                    i = j;            
                }
                perm1[r] = p0; 
           
                count[r] -= 1;         
                if (count[r] > 0) more = false; else r += 1;  
            }
            nperm += 1;
        }
        return flips;      
    }
}

