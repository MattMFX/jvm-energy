# JVM Energy Measurement Benchmarks with jRAPL

This project provides energy consumption benchmarks for Java sorting algorithms using **jRAPL** (Java RAPL) for real-time power measurement and **JMH** (Java Microbenchmark Harness) for accurate performance benchmarking.

## 🔋 What it Measures

- **Real Energy Consumption**: Actual power consumption in Joules using Intel RAPL counters
- **Performance Metrics**: Execution time, throughput, and statistical analysis via JMH
- **Algorithm Comparison**: Bubble Sort, Merge Sort, and Quick Sort energy efficiency

## 📋 Prerequisites

### Hardware Requirements
- **Intel CPU with RAPL support** (Skylake, Kaby Lake, Coffee Lake, or newer)
- **Linux system** with MSR (Model Specific Register) support
- **Root privileges** for accessing hardware energy counters

### Software Requirements
- **Java 8+** (OpenJDK or Oracle JDK)
- **Maven 3.6+** for build management
- **GCC compiler** for native library compilation
- **Make** for building jRAPL native components

## 🚀 Quick Start

### 1. Clone and Setup
```bash
git clone <your-repo-url>
cd jvm-energy
```

### 2. Build Native Libraries
```bash
cd jRAPL
make clean
make all
cp *.so ../
cd ..
```

### 3. Compile Java Code
```bash
mvn clean compile package
```

### 4. Enable MSR Module (Required for Energy Measurement)
```bash
sudo modprobe msr
```

### 5. Run Benchmarks
```bash
# Run all sorting algorithms with energy measurement
sudo java -Djava.library.path=. -Djmh.ignoreLock=true -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark

# Run specific algorithm (faster for testing)
sudo java -Djava.library.path=. -Djmh.ignoreLock=true -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark.quick_sort_energy
```

## 📊 Understanding the Output

### Benchmark Results
```
Benchmark                                            Mode  Cnt     Score    Error  Units
EnergyMeasuredSortingBenchmark.bubble_sort_energy   avgt    5   190.451 ± 45.231  ms/op
EnergyMeasuredSortingBenchmark.merge_sort_energy    avgt    5     4.083 ±  0.892  ms/op
EnergyMeasuredSortingBenchmark.quick_sort_energy    avgt    5     2.912 ±  0.456  ms/op
```

### Energy Consumption Logs
```
energy,algo=quick_sort,size=10000,joules=0.009216000
energy,algo=merge_sort,size=10000,joules=0.012455000
energy,algo=bubble_sort,size=10000,joules=0.087432000
```

**Interpretation:**
- **Score**: Average execution time per operation
- **Error**: Statistical confidence interval
- **Energy logs**: Actual energy consumption in Joules per operation

## ⚙️ Configuration Options

### Benchmark Parameters
```bash
# Custom warmup iterations, benchmark iterations, and forks
sudo java -Djava.library.path=. -Djmh.ignoreLock=true -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark \
  -wi 3 \    # Warmup iterations
  -i 5 \     # Benchmark iterations  
  -f 2       # Number of forks
```

### Array Size Modification
Edit `EnergyMeasuredSortingBenchmark.java`:
```java
private static final int ARRAY_SIZE = 10000; // Change this value
```

### JMH Output Formats
```bash
# JSON output
sudo java -Djava.library.path=. -Djmh.ignoreLock=true -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark -rf JSON

# CSV output
sudo java -Djava.library.path=. -Djmh.ignoreLock=true -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark -rf CSV
```

## 🏗️ Project Structure

```
jvm-energy/
├── src/main/java/
│   └── dev/matheus/energy/
│       ├── EnergyMeasuredSortingBenchmark.java  # Main benchmark class
│       ├── EnergyLog.java                       # Energy logging utility
│       ├── SortingAlgorithms.java              # Algorithm implementations
│       └── jrapl/
│           └── EnergyCheckUtils.java           # jRAPL Java interface
├── jRAPL/                                      # Native jRAPL library
│   ├── CPUScaler.c                            # Main energy measurement code
│   ├── arch_spec.h                            # CPU architecture definitions
│   ├── EnergyCheckUtils.java                  # Original jRAPL interface
│   ├── PerfCheckUtils.java                    # Performance counters
│   └── Makefile                               # Build configuration
├── pom.xml                                     # Maven configuration
└── README.md                                   # This file
```

## 🔧 Troubleshooting

### "ProfileInit failed with return code -1"
**Problem**: jRAPL cannot initialize energy measurement  
**Solutions**:
1. Ensure you're running with `sudo`
2. Load MSR module: `sudo modprobe msr`
3. Verify CPU supports RAPL: `cat /proc/cpuinfo | grep model`

### "UnsatisfiedLinkError"
**Problem**: Native library not found  
**Solutions**:
1. Ensure `.so` files are in project root: `ls -la *.so`
2. Verify library path: `-Djava.library.path=.`
3. Rebuild native libraries: `cd jRAPL && make clean && make all`

### "Architecture not detected"
**Problem**: Your CPU model isn't recognized by jRAPL  
**Solution**: Check if your CPU model is defined in `jRAPL/arch_spec.h`. We've added support for:
- Kaby Lake (models 0x8E, 0x9E)
- Skylake, Broadwell, and newer

### Energy Values Show as 0 or NaN
**Problem**: Energy measurement not working but benchmark runs  
**Causes**:
1. **Virtual Machine**: RAPL not available in VMs
2. **Non-Intel CPU**: AMD CPUs use different interfaces
3. **Old CPU**: Pre-Sandy Bridge CPUs lack RAPL support
4. **Permission Issues**: MSR access denied

### Performance Issues
**Problem**: Benchmark takes too long  
**Solutions**:
1. Reduce iterations: `-wi 1 -i 3 -f 1`
2. Test single algorithm: `EnergyMeasuredSortingBenchmark.quick_sort_energy`
3. Reduce array size in source code

## 📊 Energy Data Analysis with Python

After running benchmarks, analyze your energy measurement results using the included Python tools:

### Quick Analysis (No Dependencies Required)
```bash
python3 energy_analyzer_simple.py target/energy.csv --output-dir analysis_results
```

### Advanced Analysis with Visualizations
```bash
# Install dependencies
pip install -r requirements.txt

# Run advanced analyzer with plots
python3 energy_analyzer.py target/energy.csv --output-dir analysis_results
```

**Analysis Features:**
- 📈 Comprehensive statistical reports
- 🔬 Algorithm performance comparison  
- ⚡ Energy efficiency ratings
- 📊 Beautiful visualizations (advanced version)
- 💾 Enhanced CSV exports with additional metrics

See [`PYTHON_ANALYSIS.md`](PYTHON_ANALYSIS.md) for detailed usage instructions.

## 📈 Extending the Benchmarks

### Adding New Sorting Algorithms
1. Add implementation to `SortingAlgorithms.java`
2. Add benchmark method to `EnergyMeasuredSortingBenchmark.java`:
```java
@Benchmark
public void your_algorithm_energy(Blackhole bh) {
    // Your benchmark implementation
}
```

### Adding Different Workloads
1. Create new benchmark class extending the energy measurement pattern
2. Import `EnergyCheckUtils` and `EnergyLog`
3. Follow the measurement pattern from existing benchmarks

## 🧪 Verification

### Test jRAPL Integration
```bash
# Quick test of energy measurement
sudo java -Djava.library.path=. -cp target/classes \
  -c "import dev.matheus.energy.jrapl.EnergyCheckUtils; \
      System.out.println(\"Sockets: \" + EnergyCheckUtils.GetSocketNum()); \
      System.out.println(\"Energy: \" + EnergyCheckUtils.getEnergyStats());"
```

### Validate CPU Support
```bash
# Check CPU model
cat /proc/cpuinfo | grep "model name" | head -1

# Check if RAPL files exist
ls -la /sys/class/powercap/intel-rapl*/
```

## 📜 License

This project incorporates jRAPL library components. Please refer to the original jRAPL license for native library usage terms.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Add tests for new algorithms or measurements
4. Submit pull request with detailed description

## 📞 Support

For issues related to:
- **Energy measurement**: Verify CPU compatibility and MSR access
- **Benchmark accuracy**: Review JMH documentation and warm-up settings
- **Native compilation**: Check GCC version and system libraries

---

**Happy Benchmarking! 🚀⚡**