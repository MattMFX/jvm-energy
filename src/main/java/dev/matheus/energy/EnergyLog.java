package dev.matheus.energy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class EnergyLog {

    private static final String TARGET_FILE;
    private static volatile boolean headerWritten = false;
    
    static {
        // Generate filename with timestamp: energy_YYYY-MM-DD_HH-mm.csv
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
        String timestamp = LocalDateTime.now().format(formatter);
        TARGET_FILE = "results/energy_" + timestamp + ".csv";
    }

    static synchronized void append(String algorithm, int size, double joules, double timeMs) {
        ensureHeader();
        writeLine(algorithm + "," + size + "," + format(joules) + "," + format(timeMs));
    }
    
    // Keep backward compatibility for existing code
    static synchronized void append(String algorithm, int size, double joules) {
        append(algorithm, size, joules, Double.NaN);
    }

    private static void ensureHeader() {
        if (headerWritten) return;
        try {
            Path p = Path.of(TARGET_FILE);
            if (!Files.exists(p)) {
                File parent = p.toFile().getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                writeRaw("algo,size,joules,time_ms\n", false);
            }
            headerWritten = true;
        } catch (Exception ignored) {
        }
    }

    private static void writeLine(String line) {
        writeRaw(line + "\n", true);
    }

    private static void writeRaw(String content, boolean append) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(TARGET_FILE, append))) {
            bw.write(content);
        } catch (IOException ignored) {
        }
    }

    private static String format(double d) {
        return String.format(java.util.Locale.ROOT, "%.9f", d);
    }
}


