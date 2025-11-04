package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.util.concurrent.CyclicBarrier;

/**
 * SpectralNorm benchmark variant #2 from the Computer Language Benchmarks Game.
 * Based on C# entry by Isaac Gouy, contributed by Jarkko Miettinen, parallel by The Anh Tran.
 * 
 * Source: benchmarksgame/spectralnorm/spectralnorm.java-2.java
 */
public class SpectralNormBenchmark_V2 implements BenchmarkAlgorithm {
    
    @Override
    public String getName() {
        return "spectralnorm_v2";
    }
    
    @Override
    public String getDescription() {
        return "Spectral norm calculation (parallel variant 2)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // SpectralNorm: n=5500
        return 5500;
    }
    
    @Override
    public Object execute(int n) throws Exception {
        // create unit vector
        double[] u = new double[n];
        double[] v = new double[n];
        double[] tmp = new double[n];
        
        for (int i = 0; i < n; i++)
            u[i] = 1.0;
        
        // get available processor, then set up syn object
        int nthread = Runtime.getRuntime().availableProcessors();
        CyclicBarrier barrier = new CyclicBarrier(nthread);
        
        int chunk = n / nthread;
        Approximate[] ap = new Approximate[nthread];
        
        for (int i = 0; i < nthread; i++) {
            int r1 = i * chunk;
            int r2 = (i < (nthread - 1)) ? r1 + chunk : n;
            
            ap[i] = new Approximate(u, v, tmp, r1, r2, barrier);
        }
        
        double vBv = 0, vv = 0;
        for (int i = 0; i < nthread; i++) {
            try {
                ap[i].join();
                
                vBv += ap[i].m_vBv;
                vv += ap[i].m_vv;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return Math.sqrt(vBv / vv);
    }
    
    private static class Approximate extends Thread {
        private final CyclicBarrier barrier;
        private final double[] _u;
        private final double[] _v;
        private final double[] _tmp;
        private final int range_begin, range_end;
        private double m_vBv = 0, m_vv = 0;
        
        public Approximate(double[] u, double[] v, double[] tmp, int rbegin, int rend, CyclicBarrier barrier) {
            super();
            
            _u = u;
            _v = v;
            _tmp = tmp;
            
            range_begin = rbegin;
            range_end = rend;
            this.barrier = barrier;
            
            start();
        }
        
        public void run() {
            // 20 steps of the power method
            for (int i = 0; i < 10; i++) {
                MultiplyAtAv(_u, _tmp, _v);
                MultiplyAtAv(_v, _tmp, _u);
            }
            
            for (int i = range_begin; i < range_end; i++) {
                m_vBv += _u[i] * _v[i];
                m_vv += _v[i] * _v[i];
            }
        }
        
        /* return element i,j of infinite matrix A */
        private static double eval_A(int i, int j) {
            int div = (((i + j) * (i + j + 1) >>> 1) + i + 1);
            return 1.0 / div;
        }
        
        /* multiply vector v by matrix A, each thread evaluate its range only */
        private void MultiplyAv(final double[] v, double[] Av) {
            for (int i = range_begin; i < range_end; i++) {
                double sum = 0;
                for (int j = 0; j < v.length; j++)
                    sum += eval_A(i, j) * v[j];
                
                Av[i] = sum;
            }
        }
        
        /* multiply vector v by matrix A transposed */
        private void MultiplyAtv(final double[] v, double[] Atv) {
            for (int i = range_begin; i < range_end; i++) {
                double sum = 0;
                for (int j = 0; j < v.length; j++)
                    sum += eval_A(j, i) * v[j];
                
                Atv[i] = sum;
            }
        }
        
        /* multiply vector v by matrix A and then by matrix A transposed */
        private void MultiplyAtAv(final double[] v, double[] tmp, double[] AtAv) {
            try {
                MultiplyAv(v, tmp);
                // all thread must syn at completion
                barrier.await();
                MultiplyAtv(tmp, AtAv);
                // all thread must syn at completion
                barrier.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

