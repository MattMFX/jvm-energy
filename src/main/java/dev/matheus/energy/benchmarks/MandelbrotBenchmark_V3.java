package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mandelbrot benchmark variant #3 from the Computer Language Benchmarks Game.
 * Contributed by Stefan Krause, slightly modified by Chad Whipkey,
 * parallelized by Colin D Bennett, reduce synchronization cost by The Anh Tran.
 * 
 * Source: benchmarksgame/mandelbrot/mandelbrot.java-3.java
 */
public class MandelbrotBenchmark_V3 implements BenchmarkAlgorithm {
    
    @Override
    public String getName() {
        return "mandelbrot_v3";
    }
    
    @Override
    public String getDescription() {
        return "Mandelbrot set generation (variant 3)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Mandelbrot: 16000
        return 1000;
    }
    
    @Override
    public Object execute(int size) throws Exception {
        byte[][] outputData = new byte[size][(size / 8) + 1];
        int[] bytesPerLine = new int[size];
        
        compute(size, outputData, bytesPerLine);
        
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        for (int i = 0; i < size; i++) {
            ostream.write(outputData[i], 0, bytesPerLine[i]);
        }
        
        return ostream.toByteArray();
    }
    
    private static void compute(final int N, final byte[][] output, final int[] bytesPerLine) {
        final double inverseN = 2.0 / N;
        final AtomicInteger currentLine = new AtomicInteger(0);
        
        final Thread[] pool = new Thread[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Thread() {
                public void run() {
                    int y;
                    while ((y = currentLine.getAndIncrement()) < N) {
                        byte[] pdata = output[y];
                        
                        int bitNum = 0;
                        int byteCount = 0;
                        int byteAccumulate = 0;
                        
                        double Civ = (double) y * inverseN - 1.0;
                        for (int x = 0; x < N; x++) {
                            double Crv = (double) x * inverseN - 1.5;
                            
                            double Zrv = Crv;
                            double Ziv = Civ;
                            
                            double Trv = Crv * Crv;
                            double Tiv = Civ * Civ;
                            
                            int i = 49;
                            do {
                                Ziv = (Zrv * Ziv) + (Zrv * Ziv) + Civ;
                                Zrv = Trv - Tiv + Crv;
                                
                                Trv = Zrv * Zrv;
                                Tiv = Ziv * Ziv;
                            } while (((Trv + Tiv) <= 4.0) && (--i > 0));
                            
                            byteAccumulate <<= 1;
                            if (i == 0) byteAccumulate++;
                            
                            if (++bitNum == 8) {
                                pdata[byteCount++] = (byte) byteAccumulate;
                                bitNum = byteAccumulate = 0;
                            }
                        }
                        
                        if (bitNum != 0) {
                            byteAccumulate <<= (8 - (N & 7));
                            pdata[byteCount++] = (byte) byteAccumulate;
                        }
                        
                        bytesPerLine[y] = byteCount;
                    }
                }
            };
            
            pool[i].start();
        }
        
        for (Thread t : pool) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

