package dev.matheus.energy.benchmarks;

import dev.matheus.energy.BenchmarkAlgorithm;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fasta benchmark variant #6 from the Computer Language Benchmarks Game.
 * Modified by Mehmet D. AKIN, Daryl Griffith, and Mike.
 * This is a more complex parallel version with buffered processing.
 * 
 * Source: benchmarksgame/fasta/fasta.java-6.java
 */
public class FastaBenchmark_V6 implements BenchmarkAlgorithm {
    
    private static final int LINE_LENGTH = 60;
    private static final int LINE_COUNT = 1024;
    
    @Override
    public String getName() {
        return "fasta_v6";
    }
    
    @Override
    public String getDescription() {
        return "DNA sequence generation (parallel variant 6)";
    }
    
    @Override
    public int getEffectiveSize(int requestedSize) {
        return 2_500_000;
    }
    
    @Override
    public Object execute(int n) throws Exception {
        final int BUFFERS_IN_PLAY = 6;
        final int WORKERS_COUNT = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        
        NucleotideSelector[] workers = new NucleotideSelector[WORKERS_COUNT];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new NucleotideSelector();
            workers[i].setDaemon(true);
            workers[i].start();
        }
        
        AtomicInteger IN = new AtomicInteger();
        AtomicInteger OUT = new AtomicInteger();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bufferSize = LINE_COUNT * LINE_LENGTH;
        
        // Process ALU
        for (int i = 0; i < BUFFERS_IN_PLAY; i++) {
            workers[OUT.incrementAndGet() % workers.length].put(
                new AluBuffer(LINE_LENGTH, bufferSize, i * bufferSize));
        }
        speciesFillALU(out, n * 2, workers, IN, OUT, bufferSize, BUFFERS_IN_PLAY);
        
        // Process IUB
        for (int i = 0; i < BUFFERS_IN_PLAY; i++) {
            AbstractBuffer buffer = workers[IN.incrementAndGet() % workers.length].take();
            out.write(buffer.nucleotides);
            StochasticBuffer stochBuffer = new IubBuffer(LINE_LENGTH, bufferSize);
            stochBuffer.fillRandoms();
            workers[OUT.incrementAndGet() % workers.length].put(stochBuffer);
        }
        speciesFillRandom(out, n * 3, workers, IN, OUT, bufferSize, BUFFERS_IN_PLAY, true);
        
        // Process Sapien
        for (int i = 0; i < BUFFERS_IN_PLAY; i++) {
            AbstractBuffer buffer = workers[IN.incrementAndGet() % workers.length].take();
            out.write(buffer.nucleotides);
            StochasticBuffer stochBuffer = new SapienBuffer(LINE_LENGTH, bufferSize);
            stochBuffer.fillRandoms();
            workers[OUT.incrementAndGet() % workers.length].put(stochBuffer);
        }
        speciesFillRandom(out, n * 5, workers, IN, OUT, bufferSize, BUFFERS_IN_PLAY, false);
        
        for (int i = 0; i < BUFFERS_IN_PLAY; i++) {
            out.write(workers[IN.incrementAndGet() % workers.length].take().nucleotides);
        }
        
        // Stop workers
        for (NucleotideSelector worker : workers) {
            worker.interrupt();
        }
        
        return out.toByteArray();
    }
    
    private void speciesFillALU(OutputStream writer, int nChars, NucleotideSelector[] workers,
            AtomicInteger IN, AtomicInteger OUT, int bufferSize, int BUFFERS_IN_PLAY) throws IOException {
        int bufferCount = nChars / (LINE_COUNT * LINE_LENGTH);
        int charsLeftover = nChars % (LINE_COUNT * LINE_LENGTH);
        
        for (int i = 0; i < bufferCount - BUFFERS_IN_PLAY; i++) {
            AbstractBuffer buffer = workers[IN.incrementAndGet() % workers.length].take();
            writer.write(buffer.nucleotides);
            workers[OUT.incrementAndGet() % workers.length].put(
                new AluBuffer(LINE_LENGTH, bufferSize, (i + BUFFERS_IN_PLAY) * bufferSize));
        }
        
        if (charsLeftover > 0) {
            AbstractBuffer buffer = workers[IN.incrementAndGet() % workers.length].take();
            writer.write(buffer.nucleotides);
            workers[OUT.incrementAndGet() % workers.length].put(
                new AluBuffer(LINE_LENGTH, charsLeftover, nChars - charsLeftover));
        }
    }
    
    private void speciesFillRandom(OutputStream writer, int nChars, NucleotideSelector[] workers,
            AtomicInteger IN, AtomicInteger OUT, int bufferSize, int BUFFERS_IN_PLAY,
            boolean isIUB) throws IOException {
        int bufferCount = nChars / bufferSize;
        int bufferLoops = bufferCount - BUFFERS_IN_PLAY;
        int charsLeftover = nChars - (bufferCount * bufferSize);
        
        for (int i = 0; i < bufferLoops; i++) {
            AbstractBuffer buffer = workers[IN.incrementAndGet() % workers.length].take();
            writer.write(buffer.nucleotides);
            StochasticBuffer stochBuffer = isIUB ? 
                new IubBuffer(LINE_LENGTH, bufferSize) : 
                new SapienBuffer(LINE_LENGTH, bufferSize);
            stochBuffer.fillRandoms();
            workers[OUT.incrementAndGet() % workers.length].put(stochBuffer);
        }
        
        if (charsLeftover > 0) {
            AbstractBuffer buffer = workers[IN.incrementAndGet() % workers.length].take();
            writer.write(buffer.nucleotides);
            StochasticBuffer stochBuffer = isIUB ? 
                new IubBuffer(LINE_LENGTH, charsLeftover) : 
                new SapienBuffer(LINE_LENGTH, charsLeftover);
            stochBuffer.fillRandoms();
            workers[OUT.incrementAndGet() % workers.length].put(stochBuffer);
        }
    }
    
    private static class NucleotideSelector extends Thread {
        private final BlockingQueue<AbstractBuffer> in = new ArrayBlockingQueue<>(6);
        private final BlockingQueue<AbstractBuffer> out = new ArrayBlockingQueue<>(6);

        public void put(AbstractBuffer line) {
            try {
                in.put(line);
            } catch (InterruptedException ex) {
                // Ignore
            }
        }

        @Override
        public void run() {
            try {
                for (;;) {
                    AbstractBuffer line = in.take();
                    line.selectNucleotides();
                    out.put(line);
                }
            } catch (InterruptedException ex) {
                // Thread interrupted, exit
            }
        }

        public AbstractBuffer take() {
            try {
                return out.take();
            } catch (InterruptedException ex) {
                return null;
            }
        }
    }
    
    private abstract static class AbstractBuffer {
        protected final int LINE_LENGTH;
        protected final int LINE_COUNT;
        protected final byte[] nucleotides;
        protected final int CHARS_LEFTOVER;

        AbstractBuffer(int lineLength, int nChars) {
            LINE_LENGTH = lineLength;
            int outputLineLength = lineLength + 1;
            LINE_COUNT = nChars / lineLength;
            CHARS_LEFTOVER = nChars % lineLength;
            int nucleotidesSize = nChars + LINE_COUNT + (CHARS_LEFTOVER == 0 ? 0 : 1);
            int lastNucleotide = nucleotidesSize - 1;

            nucleotides = new byte[nucleotidesSize];
            for (int i = lineLength; i < lastNucleotide; i += outputLineLength) {
                nucleotides[i] = '\n';
            }
            nucleotides[nucleotides.length - 1] = '\n';
        }

        abstract void selectNucleotides();
    }
    
    private static class AluBuffer extends AbstractBuffer {
        private static final String ALU =
            "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG"
            + "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA"
            + "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT"
            + "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA"
            + "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG"
            + "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC"
            + "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA";
        private static final int ALU_LENGTH = ALU.length();

        private final int MAX_ALU_INDEX = ALU_LENGTH - LINE_LENGTH;
        private final int ALU_ADJUST = LINE_LENGTH - ALU_LENGTH;
        private final int nChars;
        private int charIndex;
        private int nucleotideIndex;
        private final byte[] chars;

        public AluBuffer(int lineLength, int nChars, int offset) {
            super(lineLength, nChars);
            this.nChars = nChars;
            chars = (ALU + ALU.substring(0, LINE_LENGTH)).getBytes();
            charIndex = offset % ALU_LENGTH;
        }

        @Override
        void selectNucleotides() {
            nucleotideIndex = 0;
            for (int i = 0; i < LINE_COUNT; i++) {
                ALUFillLine(LINE_LENGTH);
            }
            if (CHARS_LEFTOVER > 0) {
                ALUFillLine(CHARS_LEFTOVER);
            }
            charIndex += nChars * 5; // BUFFERS_IN_PLAY - 1
            charIndex %= ALU_LENGTH;
        }

        private void ALUFillLine(int charCount) {
            System.arraycopy(chars, charIndex, nucleotides, nucleotideIndex, charCount);
            charIndex += charIndex < MAX_ALU_INDEX ? charCount : ALU_ADJUST;
            nucleotideIndex += charCount + 1;
        }
    }
    
    private static abstract class StochasticBuffer extends AbstractBuffer {
        private static final int IM = 139968;
        private static final int IA = 3877;
        private static final int IC = 29573;
        private static final float ONE_OVER_IM = 1f / IM;
        private static int last = 42;

        protected final float[] randoms;

        protected StochasticBuffer(int lineLength, int nChars) {
            super(lineLength, nChars);
            randoms = new float[nChars];
        }

        void fillRandoms() {
            for (int i = 0; i < randoms.length; i++) {
                last = (last * IA + IC) % IM;
                randoms[i] = last * ONE_OVER_IM;
            }
        }
    }
    
    private static final class IubBuffer extends StochasticBuffer {
        private static final byte[] chars = new byte[]{
            'a', 'c', 'g', 't',
            'B', 'D', 'H', 'K',
            'M', 'N', 'R', 'S',
            'V', 'W', 'Y'};
        private static final float[] probs = new float[15];
        static {
            double[] dblProbs = new double[]{
                0.27, 0.12, 0.12, 0.27,
                0.02, 0.02, 0.02, 0.02,
                0.02, 0.02, 0.02, 0.02,
                0.02, 0.02, 0.02};

            double cp = 0;
            for (int i = 0; i < probs.length - 1; i++) {
                cp += dblProbs[i];
                probs[i] = (float) cp;
            }
            probs[probs.length - 1] = 2;
        }

        private final int charsInFullLines;

        IubBuffer(int lineLength, int nChars) {
            super(lineLength, nChars);
            charsInFullLines = (nChars / lineLength) * lineLength;
        }

        @Override
        void selectNucleotides() {
            int i = 0, j = 0;
            for (; i < charsInFullLines; j++) {
                for (int k = 0; k < LINE_LENGTH; k++)
                    nucleotides[j++] = convert(randoms[i++]);
            }
            for (int k = 0; k < CHARS_LEFTOVER; k++)
                nucleotides[j++] = convert(randoms[i++]);
        }

        private static byte convert(float r) {
            int m;
            for (m = 0; probs[m] < r; m++) {}
            return chars[m];
        }
    }
    
    private static final class SapienBuffer extends StochasticBuffer {
        private static final byte[] chars = new byte[]{'a', 'c', 'g', 't'};
        private static final float[] probs = new float[4];
        static {
            double[] dblProbs = new double[]{
                0.3029549426680,
                0.1979883004921,
                0.1975473066391,
                0.3015094502008};

            double cp = 0;
            for (int i = 0; i < probs.length - 1; i++) {
                cp += dblProbs[i];
                probs[i] = (float) cp;
            }
            probs[probs.length - 1] = 2;
        }

        private final int charsInFullLines;

        SapienBuffer(int lineLength, int nChars) {
            super(lineLength, nChars);
            charsInFullLines = (nChars / lineLength) * lineLength;
        }

        @Override
        void selectNucleotides() {
            int i = 0, j = 0;
            for (; i < charsInFullLines; j++) {
                for (int k = 0; k < LINE_LENGTH; k++)
                    nucleotides[j++] = convert(randoms[i++]);
            }
            for (int k = 0; k < CHARS_LEFTOVER; k++)
                nucleotides[j++] = convert(randoms[i++]);
        }

        private static byte convert(float r) {
            int m;
            for (m = 0; probs[m] < r; m++) {}
            return chars[m];
        }
    }
}

