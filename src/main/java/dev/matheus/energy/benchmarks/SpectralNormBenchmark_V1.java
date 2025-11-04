package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;

/**
 * SpectralNorm benchmark from the Computer Language Benchmarks Game.
 * Calculates the spectral norm of an infinite matrix.
 * 
 * Source: benchmarksgame/spectralnorm/spectralnorm.java
 * Contributed by Java novice Jarkko Miettinen
 * Modified ~3 lines of the original C#-version by Isaac Gouy
 */
public class SpectralNormBenchmark_V1 implements BenchmarkAlgorithm {
    
    @Override
    public String getName() {
        return "spectralnorm";
    }
    
    @Override
    public String getDescription() {
        return "Spectral norm calculation";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Spectral norm: 3000x3000 matrix
        return 3000;
    }
    
    @Override
    public Object execute(int n) throws Exception {
        return approximate(n);
    }
    
    private double approximate(int n) {
        // create unit vector
        double[] u = new double[n];
        for (int i = 0; i < n; i++) u[i] = 1;
        
        // 20 steps of the power method
        double[] v = new double[n];
        for (int i = 0; i < n; i++) v[i] = 0;
        
        for (int i = 0; i < 10; i++) {
            multiplyAtAv(n, u, v);
            multiplyAtAv(n, v, u);
        }
        
        // B=AtA         A multiplied by A transposed
        // v.Bv /(v.v)   eigenvalue of v
        double vBv = 0, vv = 0;
        for (int i = 0; i < n; i++) {
            vBv += u[i] * v[i];
            vv += v[i] * v[i];
        }
        
        return Math.sqrt(vBv / vv);
    }
    
    /* return element i,j of infinite matrix A */
    private double A(int i, int j) {
        return 1.0 / ((i + j) * (i + j + 1) / 2 + i + 1);
    }
    
    /* multiply vector v by matrix A */
    private void multiplyAv(int n, double[] v, double[] Av) {
        for (int i = 0; i < n; i++) {
            Av[i] = 0;
            for (int j = 0; j < n; j++) Av[i] += A(i, j) * v[j];
        }
    }
    
    /* multiply vector v by matrix A transposed */
    private void multiplyAtv(int n, double[] v, double[] Atv) {
        for (int i = 0; i < n; i++) {
            Atv[i] = 0;
            for (int j = 0; j < n; j++) Atv[i] += A(j, i) * v[j];
        }
    }
    
    /* multiply vector v by matrix A and then by matrix A transposed */
    private void multiplyAtAv(int n, double[] v, double[] AtAv) {
        double[] u = new double[n];
        multiplyAv(n, v, u);
        multiplyAtv(n, u, AtAv);
    }
}

