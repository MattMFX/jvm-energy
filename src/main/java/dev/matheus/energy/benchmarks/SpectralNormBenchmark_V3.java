package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;

/**
 * SpectralNorm benchmark variant #3 from the Computer Language Benchmarks Game.
 * Contributed by Ziad Hatahet, based on the Go entry by K P anonymous.
 * 
 * Source: benchmarksgame/spectralnorm/spectralnorm.java-3.java
 */
public class SpectralNormBenchmark_V3 implements BenchmarkAlgorithm {
    
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    
    @Override
    public String getName() {
        return "spectralnorm_v3";
    }
    
    @Override
    public String getDescription() {
        return "Spectral norm calculation (variant 3)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // SpectralNorm: n=5500
        return 5500;
    }
    
    @Override
    public Object execute(int n) throws Exception {
        double[] u = new double[n];
        for (int i = 0; i < n; i++)
            u[i] = 1.0;
        double[] v = new double[n];
        
        for (int i = 0; i < 10; i++) {
            aTimesTransp(v, u);
            aTimesTransp(u, v);
        }

        double vBv = 0.0, vv = 0.0;
        for (int i = 0; i < n; i++) {
            final double vi = v[i];
            vBv += u[i] * vi;
            vv += vi * vi;
        }
        
        return Math.sqrt(vBv / vv);
    }
    
    private static void aTimesTransp(double[] v, double[] u) throws InterruptedException {
        final double[] x = new double[u.length];
        final Thread[] t = new Thread[NCPU];
        
        for (int i = 0; i < NCPU; i++) {
            t[i] = new Times(x, i * v.length / NCPU, (i + 1) * v.length / NCPU, u, false);
            t[i].start();
        }
        for (int i = 0; i < NCPU; i++)
            t[i].join();

        for (int i = 0; i < NCPU; i++) {
            t[i] = new Times(v, i * v.length / NCPU, (i + 1) * v.length / NCPU, x, true);
            t[i].start();
        }
        for (int i = 0; i < NCPU; i++)
            t[i].join();
    }
    
    private final static class Times extends Thread {
        private final double[] v, u;
        private final int ii, n;
        private final boolean transpose;

        public Times(double[] v, int ii, int n, double[] u, boolean transpose) {
            this.v = v;
            this.u = u;
            this.ii = ii;
            this.n = n;
            this.transpose = transpose;
        }

        @Override
        public void run() {
            final int ul = u.length;
            for (int i = ii; i < n; i++) {
                double vi = 0.0;
                for (int j = 0; j < ul; j++) {
                    if (transpose)
                        vi += u[j] / a(j, i);
                    else
                        vi += u[j] / a(i, j);
                }
                v[i] = vi;
            }
        }

        private static int a(int i, int j) {
            return (i + j) * (i + j + 1) / 2 + i + 1;
        }
    }
}

