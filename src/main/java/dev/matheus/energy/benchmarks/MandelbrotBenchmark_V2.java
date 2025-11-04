package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mandelbrot benchmark variant #2 from the Computer Language Benchmarks Game.
 * Contributed by Stefan Krause, slightly modified by Chad Whipkey,
 * parallelized by Colin D Bennett, reduce synchronization cost by The Anh Tran.
 * 
 * Source: benchmarksgame/mandelbrot/mandelbrot.java-2.java
 */
public class MandelbrotBenchmark_V2 implements BenchmarkAlgorithm {
    
    @Override
    public String getName() {
        return "mandelbrot_v2";
    }
    
    @Override
    public String getDescription() {
        return "Mandelbrot set generation (optimized variant 2)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Mandelbrot: 16000
        return 1000;
    }
    
    @Override
    public Object execute(int N) throws Exception {
        double[] Crb = new double[N + 7];
        double[] Cib = new double[N + 7];
        double invN = 2.0 / N;
        for (int i = 0; i < N; i++) {
            Cib[i] = i * invN - 1.0;
            Crb[i] = i * invN - 1.5;
        }
        
        AtomicInteger yCt = new AtomicInteger();
        byte[][] out = new byte[N][(N + 7) / 8];
        
        Thread[] pool = new Thread[2 * Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Thread(() -> {
                int y;
                while ((y = yCt.getAndIncrement()) < out.length) {
                    putLine(y, out[y], Crb, Cib);
                }
            });
        }
        for (Thread t : pool) t.start();
        for (Thread t : pool) t.join();
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < N; i++) stream.write(out[i]);
        
        return stream.toByteArray();
    }
    
    private static void putLine(int y, byte[] line, double[] Crb, double[] Cib) {
        for (int xb = 0; xb < line.length; xb++) {
            line[xb] = (byte) getByte(xb * 8, y, Crb, Cib);
        }
    }
    
    private static int getByte(int x, int y, double[] Crb, double[] Cib) {
        int res = 0;
        for (int i = 0; i < 8; i += 2) {
            double Zr1 = Crb[x + i];
            double Zi1 = Cib[y];
            
            double Zr2 = Crb[x + i + 1];
            double Zi2 = Cib[y];
            
            int b = 0;
            int j = 49;
            do {
                double nZr1 = Zr1 * Zr1 - Zi1 * Zi1 + Crb[x + i];
                double nZi1 = Zr1 * Zi1 + Zr1 * Zi1 + Cib[y];
                Zr1 = nZr1;
                Zi1 = nZi1;
                
                double nZr2 = Zr2 * Zr2 - Zi2 * Zi2 + Crb[x + i + 1];
                double nZi2 = Zr2 * Zi2 + Zr2 * Zi2 + Cib[y];
                Zr2 = nZr2;
                Zi2 = nZi2;
                
                if (Zr1 * Zr1 + Zi1 * Zi1 > 4) {
                    b |= 2;
                    if (b == 3) break;
                }
                if (Zr2 * Zr2 + Zi2 * Zi2 > 4) {
                    b |= 1;
                    if (b == 3) break;
                }
            } while (--j > 0);
            res = (res << 2) + b;
        }
        return res ^ -1;
    }
}

