package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.io.ByteArrayOutputStream;

/**
 * Mandelbrot benchmark from the Computer Language Benchmarks Game.
 * Generates a Mandelbrot set image.
 * 
 * Source: benchmarksgame/mandelbrot/mandelbrot.java
 * Contributed by Stefan Krause
 * Slightly modified by Chad Whipkey
 */
public class MandelbrotBenchmark_V1 implements BenchmarkAlgorithm {
    
    @Override
    public String getName() {
        return "mandelbrot_v1";
    }
    
    @Override
    public String getDescription() {
        return "Mandelbrot set generation";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        // Mandelbrot: 1000x1000 image
        return 1000;
    }
    
    @Override
    public Object execute(int size) throws Exception {
        Mandelbrot m = new Mandelbrot(size);
        return m.compute();
    }
    
    private static class Mandelbrot {
        private static final int BUFFER_SIZE = 8192;
        
        private final int size;
        private final ByteArrayOutputStream out;
        private final byte[] buf = new byte[BUFFER_SIZE];
        private int bufLen = 0;
        private final double fac;
        private final int shift;
        
        public Mandelbrot(int size) {
            this.size = size;
            this.fac = 2.0 / size;
            this.out = new ByteArrayOutputStream();
            this.shift = size % 8 == 0 ? 0 : (8 - size % 8);
        }
        
        public byte[] compute() throws Exception {
            for (int y = 0; y < size; y++) {
                computeRow(y);
            }
            out.write(buf, 0, bufLen);
            return out.toByteArray();
        }
        
        private void computeRow(int y) throws Exception {
            int bits = 0;
            
            final double Ci = (y * fac - 1.0);
            final byte[] bufLocal = buf;
            for (int x = 0; x < size; x++) {
                double Zr = 0.0;
                double Zi = 0.0;
                double Cr = (x * fac - 1.5);
                int i = 50;
                double ZrN = 0;
                double ZiN = 0;
                do {
                    Zi = 2.0 * Zr * Zi + Ci;
                    Zr = ZrN - ZiN + Cr;
                    ZiN = Zi * Zi;
                    ZrN = Zr * Zr;
                } while (!(ZiN + ZrN > 4.0) && --i > 0);
                
                bits = bits << 1;
                if (i == 0) bits++;
                
                if (x % 8 == 7) {
                    bufLocal[bufLen++] = (byte) bits;
                    if (bufLen == BUFFER_SIZE) {
                        out.write(bufLocal, 0, BUFFER_SIZE);
                        bufLen = 0;
                    }
                    bits = 0;
                }
            }
            if (shift != 0) {
                bits = bits << shift;
                bufLocal[bufLen++] = (byte) bits;
                if (bufLen == BUFFER_SIZE) {
                    out.write(bufLocal, 0, BUFFER_SIZE);
                    bufLen = 0;
                }
            }
        }
    }
}

