# AI Agent Context: JVM Energy Measurement Project

## Project Overview

This is a **JVM energy consumption benchmarking suite** that measures the real-world power consumption of Java sorting algorithms using hardware-level energy counters. The project combines performance benchmarking (JMH) with direct hardware energy measurement (Intel RAPL via jRAPL) to provide accurate, reproducible energy efficiency metrics.

### Primary Purpose
Measure and analyze the energy consumption of different sorting algorithms (Bubble Sort, Merge Sort, Quick Sort) running on the JVM under controlled CPU frequency conditions.

### Key Innovation
Unlike traditional profiling tools that estimate energy usage, this project uses **Intel RAPL (Running Average Power Limit)** hardware counters to measure actual power consumption at the socket, CPU core, DRAM, and package levels with sub-millisecond granularity.

---

## Technology Stack

### Core Technologies
- **Java 17**: Primary application language (configured via Maven)
- **JMH 1.37** (Java Microbenchmark Harness): Provides statistically rigorous benchmarking
- **jRAPL**: Native C library for accessing Intel RAPL energy counters via JNI
- **Python 3**: Data analysis and visualization
- **GCC/Make**: Native library compilation
- **Bash**: Automation scripts for CPU management and benchmark execution

### Key Dependencies
- **Maven 3.6+**: Build and dependency management
- **libpfm4**: Performance monitoring library (for hardware counters)
- **scipy/numpy/pandas**: Python statistical analysis (optional, for advanced analysis)

---

## Architecture

### Three-Layer Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Java Application Layer (JMH Benchmarks)                │
│  - EnergyMeasuredSortingBenchmark.java                  │
│  - SortingAlgorithms.java                               │
│  - EnergyLog.java                                        │
└────────────────┬────────────────────────────────────────┘
                 │ JNI Interface
┌────────────────▼────────────────────────────────────────┐
│  Native Library Layer (jRAPL - C)                       │
│  - EnergyCheckUtils (Java JNI bridge)                   │
│  - CPUScaler.c (main energy measurement)                │
│  - arch_spec.c (CPU architecture detection)             │
│  - msr.c (Model-Specific Register access)               │
└────────────────┬────────────────────────────────────────┘
                 │ MSR/RAPL Interface
┌────────────────▼────────────────────────────────────────┐
│  Hardware Layer                                          │
│  - Intel RAPL counters (via /dev/cpu/*/msr)             │
│  - CPU frequency control (/sys/.../cpufreq/)            │
└──────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
jvm-energy/
├── src/main/java/dev/matheus/energy/
│   ├── EnergyMeasuredSortingBenchmark.java  # Main JMH benchmark class
│   │   - Defines @Benchmark methods for each algorithm
│   │   - Manages energy measurement lifecycle
│   │   - Distinguishes warmup vs measurement phases
│   │   - Logs energy consumption to CSV files
│   │
│   ├── SortingAlgorithms.java               # Algorithm implementations
│   │   - bubbleSort(int[]): O(n²) comparison-heavy
│   │   - quickSort(int[]): O(n log n) divide-and-conquer
│   │   - mergeSort(int[]): O(n log n) with memory allocation
│   │
│   ├── EnergyLog.java                       # CSV logging utility
│   │   - Thread-safe energy data persistence
│   │   - Generates timestamped CSV files: energy_YYYY-MM-DD_HH-mm.csv
│   │   - Format: algo,size,joules,time_ms
│   │
│   └── jrapl/
│       └── EnergyCheckUtils.java           # JNI bridge to native energy library
│           - init(): Initialize RAPL counters
│           - getEnergyStats(): Read current energy values
│           - ProfileDealloc(): Cleanup resources
│
├── jRAPL/                                   # Native energy measurement library
│   ├── CPUScaler.c                         # Core RAPL implementation
│   ├── arch_spec.c/.h                      # CPU architecture definitions
│   │   Supported: Skylake, Kaby Lake, Haswell, Sandy Bridge, Ivy Bridge
│   ├── msr.c/.h                            # MSR (Model-Specific Register) access
│   ├── dvfs.c                              # DVFS (Dynamic Voltage/Frequency Scaling)
│   ├── Makefile                            # Native library build configuration
│   └── libpfm/                             # Performance monitoring library
│
├── scripts/
│   ├── run_benchmark_core.sh               # Pin benchmarks to specific CPU cores
│   │   Usage: ./run_benchmark_core.sh <core_id> [jmh_args]
│   │
│   └── cpu_governor_manager.sh             # CPU frequency/governor management
│       Commands:
│       - status: Show current CPU frequencies and governors
│       - freq-info: Detailed frequency information
│       - performance: Set performance governor (max freq)
│       - frequency <mhz>: Lock CPU to specific frequency
│       - restore: Reset to default frequency range
│
├── results/                                 # Energy measurement CSV outputs
│   └── energy_YYYY-MM-DD_HH-mm_*.csv       # Timestamped benchmark results
│
├── energy_analysis.py                       # Simple statistical analysis tool
│   - Bootstrap confidence intervals (95%)
│   - Comparative efficiency rankings
│   - No external dependencies except scipy/pandas
│
├── pom.xml                                  # Maven build configuration
│   - Artifact: dev.matheus:jvm-energy:0.1.0-SNAPSHOT
│   - Produces: target/benchmarks.jar (fat JAR with JMH)
│
├── README.md                                # User documentation
└── agents.md                                # This file (AI agent context)
```

---

## Core Components Deep Dive

### 1. EnergyMeasuredSortingBenchmark.java

**Key Features:**
- JMH `@State(Scope.Thread)` ensures isolated state per benchmark thread
- `@Param({"10000"})` parameterizes array size (can be modified for different workloads)
- Tracks iteration type to distinguish warmup vs measurement phases
- Only logs energy during measurement phase (not warmup)

**Benchmark Lifecycle:**
```java
@Setup(Level.Trial)        // Once per benchmark run
→ Initialize jRAPL, allocate arrays

@Setup(Level.Iteration)    // Before each iteration set
→ Detect warmup vs measurement phase

@Setup(Level.Invocation)   // Before each benchmark invocation
→ Regenerate random data

@Benchmark                 // The measured operation
→ Copy array, measure energy, sort, log results

// Energy measurement pattern:
EnergyCheckUtils.init();
String before = EnergyCheckUtils.getEnergyStats();
long startTime = System.nanoTime();

// ACTUAL ALGORITHM EXECUTION
SortingAlgorithms.quickSort(array);

long endTime = System.nanoTime();
String after = EnergyCheckUtils.getEnergyStats();
double energyJoules = parsePackageEnergyDelta(before, after);
```

**Important Annotations:**
- `@BenchmarkMode(Mode.AverageTime)`: Reports average execution time
- `@OutputTimeUnit(TimeUnit.MILLISECONDS)`: Time in ms
- `@Warmup(iterations = 50, time = 5)`: 50 warmup iterations × 5 seconds each
- `@Measurement(iterations = 5, time = 5)`: 5 measured iterations × 5 seconds
- `@Fork(1)`: Run in a single forked JVM (prevents JVM startup bias)

### 2. jRAPL Native Library

**Energy Data Format:**
```
Raw format: "dram#cpu#package" (single socket)
            "dram#cpu#package@dram#cpu#package" (dual socket)

Parsed arrays (single socket):
- [0]: DRAM/GPU energy (depends on architecture)
- [1]: CPU core energy
- [2]: Package energy (total socket energy - most commonly used)
```

**Critical Requirements:**
- Must run with `sudo` (requires root for MSR access)
- Kernel module `msr` must be loaded: `sudo modprobe msr`
- Only works on bare metal Linux (not VMs - no MSR access)
- Intel CPU with RAPL support (Sandy Bridge or newer)
- Maximum 2 sockets supported

**Known Limitations:**
- Energy counters can wrap around (overflow) - jRAPL handles this
- Measurement granularity: ~1ms (RAPL updates counters periodically)
- Multi-socket systems less tested than single-socket

### 3. CPU Frequency Management

**Why It Matters:**
Energy consumption is highly dependent on CPU frequency (DVFS). Running benchmarks at fixed frequencies ensures reproducible results.

**Frequency Control Flow:**
```bash
# 1. Set CPU governor to 'userspace' (manual frequency control)
sudo ./scripts/cpu_governor_manager.sh userspace

# 2. Lock all CPUs to specific frequency
sudo ./scripts/cpu_governor_manager.sh frequency 2500

# 3. Run benchmark
sudo java -Djava.library.path=. -jar target/benchmarks.jar

# 4. Restore defaults
sudo ./scripts/cpu_governor_manager.sh restore
```

**CPU Governor Types:**
- `performance`: Always max frequency (high energy)
- `powersave`: Always min frequency (low performance)
- `ondemand`: Dynamic scaling based on load (unpredictable for benchmarks)
- `userspace`: Manual control (best for reproducible benchmarks)

---

## Data Flow

### Benchmark Execution Flow
```
1. Maven builds Java + generates JMH code → target/benchmarks.jar
2. JMH loads native library (libCPUScaler.so) via JNI
3. JMH runs warmup iterations (not logged)
4. JMH runs measurement iterations (logged to CSV)
5. Each benchmark invocation:
   a. Generate random array
   b. Initialize RAPL counters
   c. Read energy BEFORE
   d. Execute sorting algorithm
   e. Read energy AFTER
   f. Calculate delta (joules)
   g. Log to results/energy_<timestamp>.csv
6. JMH aggregates statistics (mean, std dev, confidence intervals)
7. Python analysis tools process CSV files
```

### Energy Measurement Precision
- **Resolution**: ~15.3 µJ per tick (microjoules)
- **Update Rate**: ~1ms intervals
- **Measurement Overhead**: ~5-10 µs per read (negligible for ms-scale algorithms)
- **Accuracy**: ±5-10% typical (hardware-dependent)

---

## Build and Run Workflow

### Initial Setup (One-Time)
```bash
# 1. Build native libraries
cd jRAPL
make clean
make all
cp *.so ../
cd ..

# 2. Compile Java and package JMH JAR
mvn clean compile package

# 3. Enable MSR access
sudo modprobe msr

# 4. Verify jRAPL works
sudo java -Djava.library.path=. -cp target/classes \
  dev.matheus.energy.jrapl.EnergyCheckUtils
```

### Running Benchmarks

**Standard Run (All Algorithms):**
```bash
sudo java -Djava.library.path=. -Djmh.ignoreLock=true \
  -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark
```

**Single Algorithm (Faster for Testing):**
```bash
sudo java -Djava.library.path=. -Djmh.ignoreLock=true \
  -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.quick_sort_energy
```

**Custom JMH Options:**
```bash
sudo java -Djava.library.path=. -Djmh.ignoreLock=true \
  -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark \
  -wi 3    # Warmup iterations (default: 50)
  -i 5     # Measurement iterations (default: 5)
  -f 1     # Forks (default: 1)
  -rf CSV  # Result format: CSV/JSON/TEXT
```

**Pin to Specific CPU Core (Isolate from OS noise):**
```bash
sudo ./scripts/run_benchmark_core.sh 0  # Run on core 0
```

### After Running - Data Analysis

**Simple Analysis (No Dependencies):**
```bash
python3 energy_analysis.py results/energy_2025-10-17_11-28*.csv
```

Output includes:
- Mean energy consumption per algorithm
- Standard deviation
- 95% confidence intervals (bootstrap method)
- Efficiency rankings
- Comparative analysis

**Generated Files:**
- `energy_report.txt`: Text summary
- `results/energy_<timestamp>.csv`: Raw measurements

---

## Common Development Tasks

### Adding a New Sorting Algorithm

**Step 1: Implement Algorithm**
```java
// In SortingAlgorithms.java
public static void heapSort(int[] array) {
    // Your implementation
}
```

**Step 2: Add Benchmark Method**
```java
// In EnergyMeasuredSortingBenchmark.java
@Benchmark
public void heap_sort_energy(Blackhole bh) throws Exception {
    int[] copy = Arrays.copyOf(data, data.length);
    double energyConsumed = Double.NaN;
    double executionTimeMs = Double.NaN;
    boolean isMeasurementPhase = currentIterationType != null 
        && currentIterationType.equals(IterationType.MEASUREMENT);
    
    if (energyAvailable) {
        try {
            EnergyCheckUtils.init();
            String before = EnergyCheckUtils.getEnergyStats();
            long startTime = System.nanoTime();
            
            SortingAlgorithms.heapSort(copy);  // NEW ALGORITHM
            
            long endTime = System.nanoTime();
            String after = EnergyCheckUtils.getEnergyStats();
            
            energyConsumed = parsePackageEnergyDelta(before, after);
            executionTimeMs = (endTime - startTime) / 1_000_000.0;
            
            if (isMeasurementPhase) {
                System.out.printf("energy,algo=%s,size=%d,joules=%.9f,time_ms=%.3f%n", 
                    "heap_sort", arraySize, energyConsumed, executionTimeMs);
                EnergyLog.append("heap_sort", arraySize, energyConsumed, executionTimeMs);
            }
        } catch (Throwable t) {
            System.err.println("Energy measurement failed: " + t.getMessage());
            // Fallback without energy measurement
            long startTime = System.nanoTime();
            SortingAlgorithms.heapSort(copy);
            long endTime = System.nanoTime();
            executionTimeMs = (endTime - startTime) / 1_000_000.0;
        }
    } else {
        // No energy measurement available
        long startTime = System.nanoTime();
        SortingAlgorithms.heapSort(copy);
        long endTime = System.nanoTime();
        executionTimeMs = (endTime - startTime) / 1_000_000.0;
    }
    
    bh.consume(copy);
    bh.consume(energyConsumed);
    bh.consume(executionTimeMs);
}
```

**Step 3: Rebuild and Test**
```bash
mvn clean compile package
sudo java -Djava.library.path=. -Djmh.ignoreLock=true \
  -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.heap_sort_energy \
  -wi 1 -i 3  # Quick test with fewer iterations
```

### Modifying Array Sizes

**Option 1: Multiple Sizes (Comprehensive)**
```java
// In EnergyMeasuredSortingBenchmark.java
@Param({"1000", "5000", "10000", "50000"})
int arraySize;
```
JMH will run benchmarks for each size, multiplying total runtime.

**Option 2: Single Size (Faster)**
```java
@Param({"10000"})
int arraySize;
```

After changing, recompile: `mvn clean compile package`

### Testing Specific CPU Frequencies

```bash
# Test at 1.5 GHz
sudo ./scripts/cpu_governor_manager.sh userspace
sudo ./scripts/cpu_governor_manager.sh frequency 1500
sudo java -Djava.library.path=. -Djmh.ignoreLock=true \
  -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark

# Test at 3.5 GHz
sudo ./scripts/cpu_governor_manager.sh frequency 3500
sudo java -Djava.library.path=. -Djmh.ignoreLock=true \
  -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark

# Compare results
python3 energy_analysis.py results/energy_*.csv
```

### Debugging Energy Measurements

**Verify RAPL Support:**
```bash
# Check CPU model
lscpu | grep "Model name"

# Check RAPL sysfs interface
ls -la /sys/class/powercap/intel-rapl*/

# Check MSR module
lsmod | grep msr
sudo modprobe msr  # If not loaded
```

**Test jRAPL Directly:**
```bash
sudo java -Djava.library.path=. -cp target/classes \
  dev.matheus.energy.jrapl.EnergyCheckUtils
# Should print: "Power consumption of dram: X power consumption of cpu: Y ..."
```

**Common Issues:**
1. **"ProfileInit failed with return code -1"**
   - Not running with sudo
   - MSR module not loaded
   - CPU doesn't support RAPL

2. **UnsatisfiedLinkError**
   - Native libraries not in project root (need `cp jRAPL/*.so .`)
   - Missing `-Djava.library.path=.` flag

3. **Energy values are 0 or NaN**
   - Running in VM (RAPL not available)
   - AMD CPU (different energy interface)
   - Pre-Sandy Bridge Intel CPU

4. **Energy values too high/inconsistent**
   - CPU frequency scaling active (set fixed frequency)
   - Background processes interfering (use `taskset` to pin core)
   - Thermal throttling (check CPU temps)

---

## Important Constraints & Considerations

### Hardware Requirements
- **MANDATORY**: Intel CPU with RAPL (Sandy Bridge 2011 or newer)
- **MANDATORY**: Bare-metal Linux (VMs don't expose MSR)
- **MANDATORY**: Root/sudo access for MSR reads
- **RECOMMENDED**: CPU frequency locking for reproducibility
- **RECOMMENDED**: Core isolation to minimize OS interference

### Software Requirements
- **Java 17+** (configured in pom.xml)
- **GCC** for native compilation
- **Linux kernel with MSR module** (standard in most distros)
- **Maven 3.6+** for Java builds

### Measurement Limitations
1. **Granularity**: ~1ms minimum measurable duration (RAPL update rate)
2. **Overhead**: Energy measurement adds ~5-10µs per read (negligible for ms+ operations)
3. **Wraparound**: Energy counters overflow; jRAPL handles this but very long measurements may lose precision
4. **Accuracy**: ±5-10% typical variation due to hardware/thermal factors
5. **Scope**: Measures full socket energy (includes OS overhead, not just algorithm)

### Best Practices for Reproducible Measurements
1. **Fix CPU frequency** before benchmarking
2. **Close background applications** (browsers, IDEs, etc.)
3. **Use core pinning** (`taskset` or `run_benchmark_core.sh`)
4. **Allow CPU cooldown** between heavy benchmarks
5. **Run multiple iterations** (JMH default: 5 measurement iterations)
6. **Monitor thermal throttling** (`watch -n1 "cat /proc/cpuinfo | grep MHz"`)
7. **Document CPU model and frequency** in results

---

## CSV Output Format

### Structure
```csv
algo,size,joules,time_ms
quick_sort,10000,0.015258000,2.912000
merge_sort,10000,0.026983000,4.083000
bubble_sort,10000,1.077768000,190.451000
```

### Fields
- **algo**: Algorithm name (quick_sort, merge_sort, bubble_sort)
- **size**: Array size parameter
- **joules**: Energy consumed (in joules, 9 decimal precision)
- **time_ms**: Execution time (in milliseconds, 3 decimal precision)

### File Naming
- Pattern: `results/energy_YYYY-MM-DD_HH-mm.csv`
- Generated per benchmark run (not per algorithm)
- Append-only within a run
- New file for each run (prevents overwriting)

---

## Benchmarking Philosophy

### Why JMH?
JMH handles critical benchmarking challenges:
1. **JIT Warmup**: Java HotSpot compiler optimizes code during execution; warmup iterations ensure stable performance
2. **Dead Code Elimination**: JMH prevents compiler from optimizing away benchmarked code
3. **Constant Folding**: JMH prevents compiler from pre-computing results
4. **Statistical Rigor**: Multiple iterations with confidence intervals
5. **JVM Forking**: Isolates benchmarks from previous runs' state

### Why RAPL?
Traditional profiling methods estimate energy; RAPL measures actual hardware counters:
1. **Hardware-Level Accuracy**: Direct from CPU power management unit
2. **Fine Granularity**: ~1ms resolution vs. OS-level tools (~100ms)
3. **Low Overhead**: MSR reads are lightweight (<10µs)
4. **Comprehensive**: Measures CPU, DRAM, package, GPU (arch-dependent)

### Why Different Algorithms?
- **Bubble Sort (O(n²))**: Memory-efficient, comparison-heavy, poor scalability
- **Quick Sort (O(n log n))**: In-place, cache-friendly, good general-purpose
- **Merge Sort (O(n log n))**: Stable, predictable, memory-intensive

These provide different energy profiles:
- Bubble: High CPU, low memory bandwidth
- Quick: Moderate CPU, moderate memory, cache-sensitive
- Merge: Lower CPU, higher memory bandwidth, more allocations

---

## Python Analysis Tools

### energy_analysis.py
**Purpose**: Simple statistical analysis without heavy dependencies

**Features:**
- Bootstrap confidence intervals (95%)
- Mean, standard deviation, sample size
- Efficiency rankings (best to worst)
- Relative comparisons (X% more efficient)

**Usage:**
```bash
python3 energy_analysis.py results/energy_2025-10-17_11-28*.csv
```

**Output Files:**
- `energy_report.txt`: Statistical summary
- Console output: Detailed per-algorithm stats

**Dependencies:**
- pandas (CSV processing)
- numpy (statistics)
- scipy (bootstrap CI)

**No Dependency Alternative:**
Could create a simple version using only Python stdlib:
```python
import csv
import statistics

data = {}
with open('results/energy_2025-10-17_11-28.csv') as f:
    for row in csv.DictReader(f):
        algo = row['algo']
        if algo not in data:
            data[algo] = []
        data[algo].append(float(row['joules']))

for algo, values in data.items():
    print(f"{algo}: mean={statistics.mean(values):.6f} J, "
          f"stdev={statistics.stdev(values):.6f} J")
```

---

## Future Extension Ideas

### Potential Enhancements
1. **More Algorithms**: Heap sort, radix sort, tim sort (Java's Arrays.sort)
2. **Different Workloads**: Matrix multiplication, string processing, compression
3. **Multi-threaded Benchmarks**: Parallel algorithms (fork-join, streams)
4. **Memory Profiling**: Correlate energy with GC pressure
5. **Different JVMs**: Compare OpenJDK, GraalVM, Azul Zing
6. **Advanced Analysis**: Machine learning to predict energy from code features
7. **Real-time Dashboard**: Web UI showing live energy metrics
8. **Automated Frequency Sweep**: Test all available frequencies automatically

### Adding CPU Performance Counters
jRAPL also supports hardware counters (cache misses, instructions, etc.):
```java
// In EnergyCheckUtils.java (already present, not currently used)
public native static void perfInit(int numEvents, int isSet);
public native static void singlePerfEventCheck(String eventNames);
// Could correlate cache misses with energy consumption
```

---

## Glossary

### Key Terms
- **RAPL (Running Average Power Limit)**: Intel CPU feature providing energy counters
- **MSR (Model-Specific Register)**: CPU-specific low-level configuration registers
- **JMH (Java Microbenchmark Harness)**: OpenJDK's official benchmarking framework
- **JNI (Java Native Interface)**: Java's mechanism to call C/C++ code
- **DVFS (Dynamic Voltage/Frequency Scaling)**: CPU power management technique
- **Wraparound**: Energy counter overflow (handled by jRAPL)
- **Blackhole**: JMH construct to prevent dead code elimination
- **Fork**: JMH feature to run benchmarks in fresh JVM instances
- **Warmup**: Pre-benchmark iterations to trigger JIT compilation

### Energy Units
- **Joule (J)**: Standard energy unit (watt × second)
- **Watt (W)**: Power unit (joules per second)
- **Energy = Power × Time**: 1 watt running for 1 second = 1 joule

### Statistical Terms
- **Mean**: Average value across all measurements
- **Standard Deviation**: Measure of variance/spread
- **Confidence Interval (CI)**: Range where true mean likely falls (95% = 95% confidence)
- **Bootstrap**: Resampling method for estimating CI without assuming distribution
- **Margin of Error**: Half-width of confidence interval

---

## Critical Context for AI Agents

### When Modifying Code
1. **Always rebuild native libraries** if changing jRAPL C code: `cd jRAPL && make clean && make all`
2. **Always recompile Java** if changing benchmark code: `mvn clean compile package`
3. **Preserve energy measurement pattern** in new benchmarks (see template in "Adding New Algorithm")
4. **Don't modify JMH annotations** without understanding JMH lifecycle (read JMH docs)
5. **Test with reduced iterations first**: `-wi 1 -i 3` before full runs

### When Analyzing Results
1. **Check CPU frequency** was stable during measurement (compare time_ms consistency)
2. **Look for outliers** (thermal throttling, OS interrupts)
3. **Verify sample size** (>30 measurements per algorithm recommended)
4. **Consider warmup** - only measurement phase is logged
5. **Account for array size** - energy scales with workload

### When Troubleshooting
1. **Start simple**: Test jRAPL standalone before full benchmark
2. **Check prerequisites**: sudo, MSR module, CPU support, native libs
3. **Isolate issues**: Run single algorithm with verbose JMH output (`-v`)
4. **Compare baselines**: Run without energy measurement to verify algorithm correctness
5. **Monitor system**: `htop`, `dmesg`, `/proc/cpuinfo` for system state

### When Extending
1. **Follow existing patterns**: Copy-paste existing benchmark methods, modify carefully
2. **Maintain CSV format**: Analysis tools depend on current schema
3. **Document assumptions**: Energy accuracy depends on many factors
4. **Version native libs**: jRAPL changes require rebuild and copy to project root
5. **Test incrementally**: Add one algorithm/feature at a time

---

## Quick Reference Commands

```bash
# Build everything from scratch
cd jRAPL && make clean && make all && cp *.so ../ && cd ..
mvn clean compile package

# Full benchmark run
sudo modprobe msr
sudo ./scripts/cpu_governor_manager.sh userspace
sudo ./scripts/cpu_governor_manager.sh frequency 2500
sudo java -Djava.library.path=. -Djmh.ignoreLock=true \
  -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark

# Quick test (fewer iterations)
sudo java -Djava.library.path=. -Djmh.ignoreLock=true \
  -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.quick_sort_energy \
  -wi 1 -i 3 -f 1

# Analyze results
python3 energy_analysis.py results/energy_*.csv

# Check CPU status
sudo ./scripts/cpu_governor_manager.sh status
sudo ./scripts/cpu_governor_manager.sh freq-info

# Cleanup
sudo ./scripts/cpu_governor_manager.sh restore
```

---

## Support & Resources

### Internal Documentation
- `README.md`: User-facing setup and usage guide
- `jRAPL/README.md`: jRAPL library documentation
- This file (`agents.md`): Comprehensive AI agent context

### External References
- **JMH**: https://github.com/openjdk/jmh
- **Intel RAPL**: https://www.intel.com/content/www/us/en/developer/articles/technical/intel-power-governor.html
- **jRAPL Project**: http://kliu20.github.io/jRAPL/
- **MSR Module**: https://www.kernel.org/doc/Documentation/x86/msr.txt

### Common Pitfalls to Avoid
1. Running in VM (RAPL won't work)
2. Forgetting sudo (MSR access denied)
3. Not loading MSR module (ProfileInit fails)
4. Not copying .so files to project root (UnsatisfiedLinkError)
5. Comparing results across different CPU frequencies
6. Ignoring warmup phase (JIT not stabilized)
7. Running with background load (CPU contention)

---

**Last Updated**: 2025-10-30
**Project Version**: 0.1.0-SNAPSHOT
**Maintained By**: Matheus (matheus@dev)


