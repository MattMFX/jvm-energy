# Refactoring Summary: Individual Algorithm Execution

## Problem
Previously, when running multiple benchmarks via `UnifiedEnergyBenchmark.java`, all algorithms shared the same JMH timing window. For example, with a 5-second measurement time, all 10+ algorithms would run collectively within those 5 seconds, not individually.

## Solution
Refactored the system so that **each algorithm gets its full JMH timing allocation**. Now, each algorithm runs in a separate JVM execution, ensuring proper warmup and measurement phases.

---

## Changes Made

### 1. **UnifiedEnergyBenchmark.java** - Simplified
**File**: `src/main/java/dev/matheus/energy/UnifiedEnergyBenchmark.java`

**Before**:
- Looped through all enabled benchmarks within a single `@Benchmark` method
- All algorithms shared the JMH timing window
- Complex iteration logic

**After**:
- Runs **exactly ONE algorithm per execution**
- Validates that only one algorithm is enabled (via filter)
- Simplified from `runAllBenchmarks()` to `runBenchmark()`
- Each algorithm gets full JMH timing (warmup + measurement)

**Key Changes**:
```java
// Before: List of benchmarks
private List<BenchmarkAlgorithm> enabledBenchmarks;

// After: Single algorithm
private BenchmarkAlgorithm algorithm;

// Before: Loop through all
for (BenchmarkAlgorithm algo : enabledBenchmarks) {
    runSingleBenchmark(algo, bh, isMeasurementPhase);
}

// After: Run single algorithm
result = algorithm.execute(effectiveSize);
```

### 2. **BenchmarkLister.java** - New Utility
**File**: `src/main/java/dev/matheus/energy/BenchmarkLister.java`

**Purpose**: Helper utility to discover which algorithms are enabled

**Features**:
- Initializes `BenchmarkConfig` to load all registered algorithms
- Accepts optional filter prefixes as command-line arguments
- Outputs one algorithm name per line (for shell script consumption)
- Sorts output for consistency

**Usage**:
```bash
# List all enabled algorithms
java -cp target/classes dev.matheus.energy.BenchmarkLister

# List algorithms matching filter
java -cp target/classes dev.matheus.energy.BenchmarkLister "nbody"
```

### 3. **run_energy_benchmark.sh** - Enhanced Orchestration
**File**: `scripts/run_energy_benchmark.sh`

**Major Changes**:

#### a) Algorithm Discovery Function
```bash
get_algorithms_to_run()
```
- Calls `BenchmarkLister` to get list of algorithms
- Respects `--filter` option
- Handles both unified and legacy sorting algorithms

#### b) Per-Algorithm Execution Function
```bash
run_algorithm_benchmark()
```
- Runs a single algorithm with full JMH parameters
- Determines benchmark class (UnifiedEnergyBenchmark vs EnergyMeasuredSortingBenchmark)
- Sets algorithm-specific filter via `-Dbenchmark.filter=${algo_name}`
- Handles both frequency stepping and single-frequency modes
- Provides detailed progress output

#### c) Updated Execution Flow
**Without Frequency Stepping**:
```
FOR each algorithm:
    - Run algorithm with full timing
```

**With Frequency Stepping**:
```
FOR each frequency:
    - Set CPU frequency
    FOR each algorithm:
        - Run algorithm with full timing at this frequency
```

#### d) Improved Output
- Shows total number of algorithms to run
- Displays progress (e.g., "Running nbody_v1 @ 2000 MHz (5/18)")
- Calculates total runs: `frequencies × algorithms`
- Better banners and formatting

---

## Benefits

### ✅ **Each Algorithm Gets Full Execution Time**
- With 5s measurement time and 10 algorithms:
  - **Before**: All 10 algorithms share 5s total
  - **After**: Each algorithm gets its own 5s = 50s total

### ✅ **Proper JMH Warmup & Measurement**
- Each algorithm goes through full warmup phase
- More accurate performance measurements
- Better energy consumption data

### ✅ **Simplified Java Code**
- `UnifiedEnergyBenchmark.java` is now simpler and easier to understand
- Single algorithm focus per execution
- Less complexity in benchmark logic

### ✅ **Flexible Shell Script**
- Can run single algorithm or multiple
- Better progress tracking
- Proper handling of frequency stepping

### ✅ **Backward Compatible**
- Legacy sorting algorithms still work
- `--filter` option works as before
- All existing command-line options preserved

---

## Usage Examples

### Run All Enabled Benchmarks
```bash
./scripts/run_energy_benchmark.sh
```
Now runs each of the ~18 enabled algorithms separately, each getting 5s measurement time.

### Run Only NBody Variants
```bash
./scripts/run_energy_benchmark.sh --filter "nbody"
```
Runs nbody_v1, nbody_v5, nbody_v8 separately.

### Run with Frequency Stepping
```bash
./scripts/run_energy_benchmark.sh --freq-start 1000 --freq-end 3000 --freq-step 500
```
- 5 frequencies: 1000, 1500, 2000, 2500, 3000 MHz
- 18 algorithms
- Total: 90 separate benchmark executions

### Run Quick Test
```bash
./scripts/run_energy_benchmark.sh -w 2 -m 1 --filter "binarytrees_v2"
```
Runs single algorithm with reduced iterations for quick testing.

---

## Migration Notes

### For Users
- **No changes needed** - existing commands work as before
- Benchmarks will take longer (expected, since each algorithm now gets full time)
- More accurate energy measurements

### For Developers
- To add new algorithms: Update `BenchmarkConfig.java` (no changes needed)
- Each algorithm is automatically discovered and run separately
- Filter mechanism works at the Java level via `BenchmarkRegistry.enableOnly()`

---

## Technical Details

### Algorithm Filtering
The filtering happens at two levels:

1. **Java Level** (`UnifiedEnergyBenchmark.setupTrial()`):
   ```java
   String benchmarkFilter = System.getProperty("benchmark.filter");
   BenchmarkRegistry.enableOnly(prefixes);
   ```

2. **Shell Level** (`get_algorithms_to_run()`):
   ```bash
   java -cp target/classes dev.matheus.energy.BenchmarkLister "$BENCHMARK_FILTER"
   ```

### Execution Flow
```
Shell Script
    └─> For each algorithm:
        └─> java -Dbenchmark.filter=<algo> -jar benchmarks.jar
            └─> UnifiedEnergyBenchmark.setupTrial()
                └─> Loads single algorithm matching filter
                    └─> runBenchmark() executes that algorithm
                        └─> Full JMH warmup + measurement phases
```

---

## Testing Recommendations

1. **Quick Test** - Single algorithm:
   ```bash
   ./scripts/run_energy_benchmark.sh --filter "binarytrees_v2" -w 2 -m 1
   ```

2. **Multiple Algorithms**:
   ```bash
   ./scripts/run_energy_benchmark.sh --filter "nbody" -w 2 -m 1
   ```

3. **With Frequency Stepping**:
   ```bash
   ./scripts/run_energy_benchmark.sh --filter "binarytrees_v2" \
       --freq-start 2000 --freq-end 2500 --freq-step 500 -w 2 -m 1
   ```

4. **Full Run** (will take significant time):
   ```bash
   ./scripts/run_energy_benchmark.sh
   ```

---

## Files Modified

1. ✅ `src/main/java/dev/matheus/energy/UnifiedEnergyBenchmark.java` - Simplified
2. ✅ `src/main/java/dev/matheus/energy/BenchmarkLister.java` - New utility
3. ✅ `scripts/run_energy_benchmark.sh` - Enhanced orchestration

## Files Unchanged
- `BenchmarkConfig.java` - Still defines all algorithms
- `BenchmarkRegistry.java` - Still manages registration
- `BenchmarkAlgorithm.java` - Interface unchanged
- All individual benchmark implementations - No changes needed

---

## Summary

The refactoring successfully addresses the original issue by:
- Simplifying the Java benchmark code
- Moving orchestration logic to the shell script
- Ensuring each algorithm gets its full JMH timing allocation
- Maintaining backward compatibility
- Improving progress tracking and output

Each algorithm now runs in isolation with proper warmup and measurement phases, resulting in more accurate and reliable energy consumption data.

