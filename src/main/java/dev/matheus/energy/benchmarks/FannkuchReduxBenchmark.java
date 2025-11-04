package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fannkuch-Redux benchmark from the Computer Language Benchmarks Game.
 * Performs indexed access to tiny integer sequences.
 * 
 * Source: benchmarksgame/fannkuchredux/fannkuchredux.java
 * Contributed by Oleg Mazurov, June 2010
 */
public class FannkuchReduxBenchmark implements BenchmarkAlgorithm {
    
    private static final int NCHUNKS = 150;
    
    @Override
    public String getName() {
        return "fannkuchredux";
    }
    
    @Override
    public String getDescription() {
        return "Fannkuch-Redux indexed access";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Fannkuch-Redux: permutation size 11
        return 10;
    }
    
    @Override
    public Object execute(int n) throws Exception {
        // n is now the actual permutation size (7-12)
        
        if (n < 0 || n > 12) {  // 13! won't fit into int
            return new int[]{-1, -1};
        }
        if (n <= 1) {
            return new int[]{0, 0};
        }
        
        int[] Fact = new int[n + 1];
        Fact[0] = 1;
        for (int i = 1; i < Fact.length; ++i) {
            Fact[i] = Fact[i - 1] * i;
        }
        
        int CHUNKSZ = (Fact[n] + NCHUNKS - 1) / NCHUNKS;
        int NTASKS = (Fact[n] + CHUNKSZ - 1) / CHUNKSZ;
        int[] maxFlips = new int[NTASKS];
        int[] chkSums = new int[NTASKS];
        AtomicInteger taskId = new AtomicInteger(0);
        
        int nthreads = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[nthreads];
        for (int i = 0; i < nthreads; ++i) {
            threads[i] = new Thread(new Worker(n, Fact, CHUNKSZ, NTASKS, maxFlips, chkSums, taskId));
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        
        int res = 0;
        for (int v : maxFlips) {
            res = Math.max(res, v);
        }
        int chk = 0;
        for (int v : chkSums) {
            chk += v;
        }
        
        return new int[]{chk, res};
    }
    
    private static class Worker implements Runnable {
        private final int n;
        private final int[] Fact;
        private final int CHUNKSZ;
        private final int NTASKS;
        private final int[] maxFlips;
        private final int[] chkSums;
        private final AtomicInteger taskId;
        
        private int[] p, pp, count;
        
        public Worker(int n, int[] Fact, int CHUNKSZ, int NTASKS, int[] maxFlips, int[] chkSums, AtomicInteger taskId) {
            this.n = n;
            this.Fact = Fact;
            this.CHUNKSZ = CHUNKSZ;
            this.NTASKS = NTASKS;
            this.maxFlips = maxFlips;
            this.chkSums = chkSums;
            this.taskId = taskId;
        }
        
        void firstPermutation(int idx) {
            for (int i = 0; i < p.length; ++i) {
                p[i] = i;
            }
            
            for (int i = count.length - 1; i > 0; --i) {
                int d = idx / Fact[i];
                count[i] = d;
                idx = idx % Fact[i];
                
                System.arraycopy(p, 0, pp, 0, i + 1);
                for (int j = 0; j <= i; ++j) {
                    p[j] = j + d <= i ? pp[j + d] : pp[j + d - i - 1];
                }
            }
        }
        
        boolean nextPermutation() {
            int first = p[1];
            p[1] = p[0];
            p[0] = first;
            
            int i = 1;
            while (++count[i] > i) {
                count[i++] = 0;
                int next = p[0] = p[1];
                for (int j = 1; j < i; ++j) {
                    p[j] = p[j + 1];
                }
                p[i] = first;
                first = next;
            }
            return true;
        }
        
        int countFlips() {
            int flips = 1;
            int first = p[0];
            if (p[first] != 0) {
                System.arraycopy(p, 0, pp, 0, pp.length);
                do {
                    ++flips;
                    for (int lo = 1, hi = first - 1; lo < hi; ++lo, --hi) {
                        int t = pp[lo];
                        pp[lo] = pp[hi];
                        pp[hi] = t;
                    }
                    int t = pp[first];
                    pp[first] = first;
                    first = t;
                } while (pp[first] != 0);
            }
            return flips;
        }
        
        void runTask(int task) {
            int idxMin = task * CHUNKSZ;
            int idxMax = Math.min(Fact[n], idxMin + CHUNKSZ);
            
            firstPermutation(idxMin);
            
            int maxflips = 1;
            int chksum = 0;
            for (int i = idxMin; ; ) {
                if (p[0] != 0) {
                    int flips = countFlips();
                    maxflips = Math.max(maxflips, flips);
                    chksum += i % 2 == 0 ? flips : -flips;
                }
                
                if (++i == idxMax) {
                    break;
                }
                
                nextPermutation();
            }
            maxFlips[task] = maxflips;
            chkSums[task] = chksum;
        }
        
        @Override
        public void run() {
            p = new int[n];
            pp = new int[n];
            count = new int[n];
            
            int task;
            while ((task = taskId.getAndIncrement()) < NTASKS) {
                runTask(task);
            }
        }
    }
}

