# JVM Energy Benchmarks

Energy measurement benchmarks using JMH and jRAPL.

## Quick Start

```bash
# Build
mvn clean package

# Run all benchmarks
./scripts/run_energy_benchmark.sh

# Results saved to: results/energy_YYYY-MM-DD_HH-mm.csv
```

## Available Benchmarks

**Sorting (3):** quick_sort, bubble_sort, merge_sort  
**Benchmarks Game (7):** nbody, spectralnorm, binarytrees, mandelbrot, fannkuchredux, fasta, knucleotide

## Common Options

```bash
# Quick test (fewer iterations)
./scripts/run_energy_benchmark.sh -w 5 -m 2

# Custom array size (affects sorting only)
./scripts/run_energy_benchmark.sh -s 50000

# CPU frequency stepping
./scripts/run_energy_benchmark.sh --freq-step 500

# Pin to CPU core
./scripts/run_energy_benchmark.sh -c 0

# Legacy: run only sorting
./scripts/run_energy_benchmark.sh -a all

# Help
./scripts/run_energy_benchmark.sh --help
```

## Adding Benchmarks

1. Create class implementing `BenchmarkAlgorithm` in `src/main/java/dev/matheus/energy/benchmarks/`
2. Register in `BenchmarkConfig.java`: `BenchmarkRegistry.register(new YourBenchmark(), true);`
3. Rebuild: `mvn clean package`

## Notes

- Requires jRAPL and sudo for energy measurement
- Non-sorting benchmarks use fixed optimal sizes (see source code)
- Sorting benchmarks respect the `-s` parameter
- Results include: algorithm, size, frequency, joules, time_ms
